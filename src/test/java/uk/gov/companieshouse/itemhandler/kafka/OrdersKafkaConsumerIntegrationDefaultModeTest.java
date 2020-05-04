package uk.gov.companieshouse.itemhandler.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;
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
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import uk.gov.companieshouse.kafka.consumer.resilience.CHConsumerType;

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
@TestPropertySource(properties="uk.gov.companieshouse.item-handler.error-consumer=false")
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
public class OrdersKafkaConsumerIntegrationDefaultModeTest {
    private static final String ORDER_RECEIVED_TOPIC = "order-received";
    private static final String ORDER_RECEIVED_TOPIC_RETRY = "order-received-retry";
    private static final String ORDER_RECEIVED_TOPIC_ERROR = "order-received-error";
    private static final String GROUP_NAME = "order-received-consumers";
    private static final String ORDER_RECEIVED_URI = "/order/ORDER-12345";
    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String brokerAddresses;

    private KafkaTemplate<String, String> template;

    private KafkaMessageListenerContainer<String, String> container;

    private BlockingQueue<ConsumerRecord<String, String>> records;

    @Autowired
    private OrdersKafkaConsumerWrapper consumerWrapper;

    @BeforeEach
    public void setUp() {
        setUpTestKafkaOrdersProducer();
        setUpTestKafkaOrdersConsumer();
    }

    @AfterEach
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
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_NAME);
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
    @DisplayName("order-received-error topic consumer does not receive message when 'error-consumer' (env var IS_ERROR_QUEUE_CONSUMER) is false")
    public void testOrdersConsumerReceivesOrderReceivedMessage1Error() throws InterruptedException, ExecutionException {
        // When
        template.send(ORDER_RECEIVED_TOPIC_ERROR, ORDER_RECEIVED_URI);

        // Then
        verifyProcessOrderReceivedNotInvoked(CHConsumerType.ERROR_CONSUMER);
    }

    private void verifyProcessOrderReceivedNotInvoked(CHConsumerType type) throws InterruptedException {
        consumerWrapper.setTestType(type);
        consumerWrapper.getLatch().await(3000, TimeUnit.MILLISECONDS);
        assertThat(consumerWrapper.getLatch().getCount(), is(equalTo(1L)));
        final String processedOrderUri = consumerWrapper.getOrderUri();
        assertThat(processedOrderUri, isEmptyOrNullString());
    }

    @Test
    @DisplayName("order-received topic consumer receives message when 'error-consumer' (env var IS_ERROR_QUEUE_CONSUMER) is false")
    public void testOrdersConsumerReceivesOrderReceivedMessage2() throws InterruptedException, ExecutionException {
        // When
        template.send(ORDER_RECEIVED_TOPIC, ORDER_RECEIVED_URI);

        // Then
        verifyProcessOrderReceivedInvoked(CHConsumerType.MAIN_CONSUMER);
    }

    @Test
    @DisplayName("order-received-retry topic consumer receives message when 'error-consumer' (env var IS_ERROR_QUEUE_CONSUMER) is false")
    public void testOrdersConsumerReceivesOrderReceivedMessage3Retry() throws InterruptedException, ExecutionException {
        // When
        template.send(ORDER_RECEIVED_TOPIC_RETRY, ORDER_RECEIVED_URI);

        // Then
        verifyProcessOrderReceivedInvoked(CHConsumerType.RETRY_CONSUMER);
    }

    private void verifyProcessOrderReceivedInvoked(CHConsumerType type) throws InterruptedException {
        consumerWrapper.setTestType(type);
        consumerWrapper.getLatch().await(3000, TimeUnit.MILLISECONDS);
        assertThat(consumerWrapper.getLatch().getCount(), is(equalTo(0L)));
        final String processedOrderUri = consumerWrapper.getOrderUri();
        assertThat(processedOrderUri, is(equalTo(ORDER_RECEIVED_URI)));
    }
}
