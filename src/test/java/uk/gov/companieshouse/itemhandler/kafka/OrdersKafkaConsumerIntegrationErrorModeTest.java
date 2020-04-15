package uk.gov.companieshouse.itemhandler.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.concurrent.ListenableFuture;
import uk.gov.companieshouse.kafka.consumer.resilience.CHConsumerType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext
@EmbeddedKafka
@TestPropertySource(properties="uk.gov.companieshouse.item-handler.error-consumer=true")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OrdersKafkaConsumerIntegrationErrorModeTest {
    private static final String ORDER_RECEIVED_TOPIC = "order-received";
    private static final String ORDER_RECEIVED_TOPIC_RETRY = "order-received-retry";
    private static final String ORDER_RECEIVED_TOPIC_ERROR = "order-received-error";
    private static final String CONSUMER_GROUP_MAIN_RETRY = "order-received-main-retry";
    private static final String CONSUMER_GROUP_MAIN_ERROR = "order-received-error";
    private static final String ORDER_RECEIVED_URI = "/order/ORDER-12345";
    @Value("${kafka.broker.addresses}")
    private String brokerAddresses;

    private KafkaTemplate<String, String> template;

    private KafkaMessageListenerContainer<String, String> container;

    private BlockingQueue<ConsumerRecord<String, String>> records;

    @Autowired
    private OrdersKafkaConsumerWrapper consumerWrapper;

    @Before
    public void setUp() {
        setUpTestKafkaOrdersProducer();
        setUpTestKafkaOrdersConsumer();
    }

    @After
    public void tearDown() {
        consumerWrapper.reset();
        container.stop();
    }

    private void setUpTestKafkaOrdersProducer() {
        final Map<String, Object> senderProperties = KafkaTestUtils.senderProps(brokerAddresses);

        senderProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        final ProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(senderProperties);

        template = new KafkaTemplate<>(producerFactory);
        template.setDefaultTopic(ORDER_RECEIVED_TOPIC);
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
    public void testOrdersConsumerReceivesOrderReceived1Message() throws InterruptedException, ExecutionException {
        // When
        final ListenableFuture<SendResult<String, String>> future =
                template.send(ORDER_RECEIVED_TOPIC, ORDER_RECEIVED_URI);

        // Then
        verifyProcessOrderReceivedNotInvoked(CHConsumerType.MAIN_CONSUMER);
    }

    @Test
    public void testOrdersConsumerReceivesOrderReceived2MessageRetry() throws InterruptedException {
        // When
        final ListenableFuture<SendResult<String, String>> future =
                template.send(ORDER_RECEIVED_TOPIC_RETRY, ORDER_RECEIVED_URI);

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
    public void testOrdersConsumerReceivesOrderReceived3MessageError() throws InterruptedException, ExecutionException {
        // When
        final ListenableFuture<SendResult<String, String>> future =
                template.send(ORDER_RECEIVED_TOPIC_ERROR, ORDER_RECEIVED_URI);

        // Then
        verifyProcessOrderReceivedInvoked(CHConsumerType.ERROR_CONSUMER);
    }

    private void verifyProcessOrderReceivedInvoked(CHConsumerType type) throws InterruptedException {
        consumerWrapper.setTestType(type);
        consumerWrapper.getLatch().await(3000, TimeUnit.MILLISECONDS);
        assertThat(consumerWrapper.getLatch().getCount(), is(equalTo(0L)));
        final String processedOrderUri = consumerWrapper.getOrderUri();
        assertThat(processedOrderUri, is(equalTo(ORDER_RECEIVED_URI)));
    }
}
