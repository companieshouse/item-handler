package uk.gov.companieshouse.itemhandler.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import uk.gov.companieshouse.kafka.consumer.resilience.CHConsumerType;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.orders.OrderReceived;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;

@SpringBootTest
@EmbeddedKafka
@TestPropertySource(properties={"uk.gov.companieshouse.item-handler.error-consumer=false",
        "certificate.order.confirmation.recipient = nobody@nowhere.com",
        "certified-copy.order.confirmation.recipient = nobody@nowhere.com",
        "missing-image-delivery.order.confirmation.recipient = nobody@nowhere.com"})
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
public class OrdersKafkaConsumerIntegrationDefaultModeTest {
    private static final String ORDER_RECEIVED_TOPIC = "order-received";
    private static final String ORDER_RECEIVED_TOPIC_RETRY = "order-received-retry";
    private static final String ORDER_RECEIVED_TOPIC_ERROR = "order-received-error";
    private static final String GROUP_NAME = "order-received-consumers";
    private static final String ORDER_RECEIVED_URI = "/order/ORDER-12345";
    private static final String ORDER_RECEIVED_MESSAGE_JSON = "{\"order_uri\": \"/order/ORDER-12345\"}";
    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String brokerAddresses;
    @Autowired
    private SerializerFactory serializerFactory;
    @Autowired
    private OrdersKafkaProducer kafkaProducer;

    private KafkaMessageListenerContainer<String, OrderReceived> container;

    private BlockingQueue<ConsumerRecord<String, String>> records;

    @Autowired
    private OrdersKafkaConsumerWrapper consumerWrapper;

    @BeforeEach
    public void setUp() {
        setUpTestKafkaOrdersConsumer();
    }

    @AfterEach
    public void tearDown() {
        container.stop();
    }

    private void setUpTestKafkaOrdersConsumer() {
        final Map<String, Object> consumerProperties = new HashMap<>();
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, OrderReceivedDeserializer.class);
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_NAME);
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerAddresses);

        final DefaultKafkaConsumerFactory<String, OrderReceived> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProperties);

        final ContainerProperties containerProperties = new ContainerProperties(
                new String[]{ORDER_RECEIVED_TOPIC, ORDER_RECEIVED_TOPIC_RETRY, ORDER_RECEIVED_TOPIC_ERROR});

        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);

        records = new LinkedBlockingQueue<>();

        container.setupMessageListener((MessageListener<String, String>) record -> {
                    records.add(record);
                });

        container.start();

        ContainerTestUtils.waitForAssignment(container, 0);
    }

    @Test
    @DirtiesContext
    @DisplayName("order-received-error topic consumer does not receive message when 'error-consumer' (env var IS_ERROR_QUEUE_CONSUMER) is false")
    public void testOrdersConsumerReceivesOrderReceivedMessage1Error() throws InterruptedException, ExecutionException, SerializationException {
        // When
        kafkaProducer.sendMessage(consumerWrapper.createMessage(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC_ERROR));

        // Then
        verifyProcessOrderReceivedNotInvoked(CHConsumerType.ERROR_CONSUMER);
    }

    private void verifyProcessOrderReceivedNotInvoked(CHConsumerType type) throws InterruptedException {
        consumerWrapper.setTestType(type);
        consumerWrapper.getLatch().await(3000, TimeUnit.MILLISECONDS);
        assertThat(consumerWrapper.getLatch().getCount(), is(equalTo(1L)));
        String processedOrderUri = consumerWrapper.getOrderUri();
        assertThat(processedOrderUri, isEmptyOrNullString());
    }

    @Test
    @DirtiesContext
    @DisplayName("order-received topic consumer receives message when 'error-consumer' (env var IS_ERROR_QUEUE_CONSUMER) is false")
    public void testOrdersConsumerReceivesOrderReceivedMessage2() throws InterruptedException, ExecutionException, SerializationException {
        // When
        kafkaProducer.sendMessage(consumerWrapper.createMessage(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC));

        // Then
        verifyProcessOrderReceivedInvoked(CHConsumerType.MAIN_CONSUMER);
    }

    @Test
    @DirtiesContext
    @DisplayName("order-received-retry topic consumer receives message when 'error-consumer' (env var IS_ERROR_QUEUE_CONSUMER) is false")
    public void testOrdersConsumerReceivesOrderReceivedMessage3Retry() throws InterruptedException, ExecutionException, SerializationException {
        // When
        kafkaProducer.sendMessage(consumerWrapper.createMessage(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC_RETRY));

        // Then
        verifyProcessOrderReceivedInvoked(CHConsumerType.RETRY_CONSUMER);
    }

    private void verifyProcessOrderReceivedInvoked(CHConsumerType type) throws InterruptedException {
        consumerWrapper.setTestType(type);
        consumerWrapper.getLatch().await(3000, TimeUnit.MILLISECONDS);
        assertThat(consumerWrapper.getLatch().getCount(), is(equalTo(0L)));
        String processedOrderUri = consumerWrapper.getOrderUri();
        assertThat(processedOrderUri, is(equalTo(ORDER_RECEIVED_MESSAGE_JSON)));
    }
}
