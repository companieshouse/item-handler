package uk.gov.companieshouse.itemhandler.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.kafka.consumer.resilience.CHConsumerType;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static uk.gov.companieshouse.itemhandler.ItemHandlerApplication.APPLICATION_NAMESPACE;

@Aspect
@Service
public class OrdersKafkaConsumerWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);
    private CountDownLatch latch = new CountDownLatch(1);
    private String orderUri;
    private CHConsumerType testType = CHConsumerType.MAIN_CONSUMER;
    @Value("${kafka.broker.addresses}")
    private String brokerAddresses;
    private KafkaTemplate<String, String> template;
    private static final String ORDER_RECEIVED_TOPIC = "order-received";
    private static final String ORDER_RECEIVED_TOPIC_RETRY = "order-received-retry";
    private static final String ORDER_RECEIVED_TOPIC_ERROR = "order-received-error";
    private static final String ORDER_RECEIVED_URI = "/order/ORDER-12345";

    /**
     * mock message processing failure scenario for main listener
     * so that the message can be published to alternate topics '-retry' and '-error'
     * @param message
     * @throws Exception
     */
    @Before(value = "execution(* uk.gov.companieshouse.itemhandler.kafka.OrdersKafkaConsumer.processOrderReceived(..)) && args(message)")
    public void beforeOrderProcessed(final String message) throws Exception {
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
    public void afterOrderProcessed(final String message){
        LOGGER.info("OrdersKafkaConsumer.processOrderReceivedRetry() @After triggered");
        latch.countDown();
        this.orderUri = message;
    }

    CountDownLatch getLatch() { return latch; }
    String getOrderUri() { return orderUri; }
    void setTestType(CHConsumerType type) { this.testType = type;}

    private void setUpTestKafkaOrdersProducerAndSendMessageToTopic() {
        final Map<String, Object> senderProperties = KafkaTestUtils.senderProps(brokerAddresses);

        senderProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        final ProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(senderProperties);

        template = new KafkaTemplate<>(producerFactory);
        if (this.testType == CHConsumerType.RETRY_CONSUMER) {
            template.setDefaultTopic(ORDER_RECEIVED_TOPIC_RETRY);
            template.send(ORDER_RECEIVED_TOPIC_RETRY, ORDER_RECEIVED_URI);
        } else if (this.testType == CHConsumerType.ERROR_CONSUMER) {
            template.setDefaultTopic(ORDER_RECEIVED_TOPIC_ERROR);
            template.send(ORDER_RECEIVED_TOPIC_ERROR, ORDER_RECEIVED_URI);
        }
    }
}
