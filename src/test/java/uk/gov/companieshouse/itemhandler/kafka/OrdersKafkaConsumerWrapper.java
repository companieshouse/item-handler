package uk.gov.companieshouse.itemhandler.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.kafka.consumer.resilience.CHConsumerType;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import static uk.gov.companieshouse.itemhandler.ItemHandlerApplication.APPLICATION_NAMESPACE;

@Aspect
@Service
public class OrdersKafkaConsumerWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);
    private CountDownLatch latch = new CountDownLatch(1);
    private String orderUri;
    private CHConsumerType testType = CHConsumerType.MAIN_CONSUMER;
    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String brokerAddresses;
    private static final String ORDER_RECEIVED_TOPIC = "order-received";
    private static final String ORDER_RECEIVED_TOPIC_RETRY = "order-received-retry";
    private static final String ORDER_RECEIVED_TOPIC_ERROR = "order-received-error";
    private static final String ORDER_RECEIVED_URI = "/order/ORDER-12345";
    @Autowired
    private OrdersKafkaProducer ordersKafkaProducer;
    @Autowired
    private OrdersKafkaConsumer ordersKafkaConsumer;
    @Autowired
    private SerializerFactory serializerFactory;

    /**
     * mock message processing failure scenario for main listener
     * so that the message can be published to alternate topics '-retry' and '-error'
     * @param message
     * @throws Exception
     */
    @Before(value = "execution(* uk.gov.companieshouse.itemhandler.kafka.OrdersKafkaConsumer.processOrderReceived(..)) && args(message)")
    public void beforeOrderProcessed(final Message message) throws Exception {
        LOGGER.info("OrdersKafkaConsumer.processOrderReceived() @Before triggered");
        if (this.testType != CHConsumerType.MAIN_CONSUMER) {
            throw new Exception("Mock main listener exception");
        }
    }

    /**
     * mock exception handler to demonstrate publishing of unprocessed
     * message to alternate topics '-retry' and '-error'
     * @param x
     * @throws Throwable
     */
    @AfterThrowing(pointcut = "execution(* uk.gov.companieshouse.itemhandler.kafka.OrdersKafkaConsumer.*(..))", throwing = "x")
    public void orderProcessedException(final Exception x) throws Throwable {
        LOGGER.info("OrdersKafkaConsumer.processOrderReceived() @AfterThrowing triggered");

        setUpTestKafkaOrdersProducerAndSendMessageToTopic();
    }

    /**
     * emulates receiving of message from kafka topics
     * @param message
     */
    @After(value = "execution(* uk.gov.companieshouse.itemhandler.kafka.OrdersKafkaConsumer.*(..)) && args(message)")
    public void afterOrderProcessed(final Message message){
        LOGGER.info("OrdersKafkaConsumer.processOrderReceivedRetry() @After triggered");
        this.orderUri = "" + message.getPayload();
        latch.countDown();
    }

    CountDownLatch getLatch() { return latch; }
    String getOrderUri() { return orderUri; }
    void setTestType(CHConsumerType type) { this.testType = type;}
    CHConsumerType getTestType() { return this.testType; }
    void reset() { this.latch = new CountDownLatch(1); }

    private void setUpTestKafkaOrdersProducerAndSendMessageToTopic()
            throws SerializationException, ExecutionException, InterruptedException {
        final Map<String, Object> senderProperties = KafkaTestUtils.senderProps(brokerAddresses);

        senderProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        final ProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(senderProperties);

        if (this.testType == CHConsumerType.MAIN_CONSUMER) {
            ordersKafkaProducer.sendMessage(ordersKafkaConsumer.createRetryMessage(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC));
        } else if (this.testType == CHConsumerType.RETRY_CONSUMER) {
            ordersKafkaProducer.sendMessage(ordersKafkaConsumer.createRetryMessage(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC_RETRY));
        } else if (this.testType == CHConsumerType.ERROR_CONSUMER) {
            ordersKafkaProducer.sendMessage(ordersKafkaConsumer.createRetryMessage(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC_ERROR));
        }
    }
}
