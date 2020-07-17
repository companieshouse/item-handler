package uk.gov.companieshouse.itemhandler.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka
@TestPropertySource(properties={"uk.gov.companieshouse.item-handler.error-consumer=true",
                                "certificate.order.confirmation.recipient = nobody@nowhere.com"})
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
public class OrdersKafkaConsumerIntegrationErrorModeTest {
    private static final String ORDER_RECEIVED_TOPIC = "order-received";
    private static final String ORDER_RECEIVED_TOPIC_RETRY = "order-received-retry";
    private static final String ORDER_RECEIVED_TOPIC_ERROR = "order-received-error";
    private static final String CONSUMER_GROUP_MAIN_RETRY = "order-received-main-retry";
    private static final String ORDER_RECEIVED_URI = "/order/ORDER-12345";
    private static final String ORDER_RECEIVED_MESSAGE_JSON = "{\"order_uri\": \"/order/ORDER-12345\"}";
    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String brokerAddresses;
    @Autowired
    private SerializerFactory serializerFactory;
    @Autowired
    private OrdersKafkaProducer kafkaProducer;

    private KafkaMessageListenerContainer<String, String> container;

    private BlockingQueue<ConsumerRecord<String, String>> records;

    @Autowired
    private OrdersKafkaConsumerWrapper consumerWrapper;

    @BeforeEach
    public void setUp() {
        setUpTestKafkaOrdersConsumer();
    }

    @AfterEach
    public void tearDown() {
        consumerWrapper.reset();
        container.stop();
    }

    private void setUpTestKafkaOrdersConsumer() {
        final Map<String, Object> consumerProperties = new HashMap<>();
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP_MAIN_RETRY);
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerAddresses);

        final DefaultKafkaConsumerFactory<String, String> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProperties);

        final ContainerProperties containerProperties = new ContainerProperties(ORDER_RECEIVED_TOPIC);

        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);

        records = new LinkedBlockingQueue<>();

        container.setupMessageListener((MessageListener<String, String>) record -> {
                    records.add(record);
                });

        container.start();

        ContainerTestUtils.waitForAssignment(container, 0);
    }

    @Test
    @DisplayName("order-received topic consumer does not receive message when 'error-consumer' (env var IS_ERROR_QUEUE_CONSUMER)is true")
    public void testOrdersConsumerReceivesOrderReceivedMessage1() throws InterruptedException, ExecutionException, SerializationException {
        // When
        kafkaProducer.sendMessage(consumerWrapper.createMessage(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC));

        // Then
        verifyProcessOrderReceivedNotInvoked(CHConsumerType.MAIN_CONSUMER);
    }

    @Test
    @DisplayName("order-received-retry topic consumer does not receive message when 'error-consumer' (env var IS_ERROR_QUEUE_CONSUMER)is true")
    public void testOrdersConsumerReceivesOrderReceivedMessage2Retry() throws InterruptedException, SerializationException, ExecutionException {
        // When
        kafkaProducer.sendMessage(consumerWrapper.createMessage(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC_RETRY));

        // Then
        verifyProcessOrderReceivedNotInvoked(CHConsumerType.RETRY_CONSUMER);
    }

    private void verifyProcessOrderReceivedNotInvoked(CHConsumerType type) throws InterruptedException {
        consumerWrapper.setTestType(type);
        consumerWrapper.getLatch().await(3000, TimeUnit.MILLISECONDS);
        assertThat(consumerWrapper.getLatch().getCount(), is(equalTo(1L)));
        final String processedOrderUri = consumerWrapper.getOrderUri();
        assertThat(processedOrderUri, isEmptyOrNullString());
    }

    @Test
    @DisplayName("order-received-error topic consumer receives message when 'error-consumer' (env var IS_ERROR_QUEUE_CONSUMER) is true")
    public void testOrdersConsumerReceivesOrderReceivedMessage3Error() throws InterruptedException, ExecutionException, SerializationException {
        // When
        kafkaProducer.sendMessage(consumerWrapper.createMessage(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC_ERROR));

        // Then
        verifyProcessOrderReceivedInvoked(CHConsumerType.ERROR_CONSUMER);
    }

    private void verifyProcessOrderReceivedInvoked(CHConsumerType type) throws InterruptedException {
        consumerWrapper.setTestType(type);
        consumerWrapper.getLatch().await(5000, TimeUnit.MILLISECONDS);
        assertThat(consumerWrapper.getLatch().getCount(), is(equalTo(0L)));
        final String processedOrderUri = consumerWrapper.getOrderUri();
        assertThat(processedOrderUri, is(equalTo(ORDER_RECEIVED_MESSAGE_JSON)));
    }
}
