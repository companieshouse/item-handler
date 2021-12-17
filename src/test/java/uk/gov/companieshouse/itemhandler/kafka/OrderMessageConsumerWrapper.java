package uk.gov.companieshouse.itemhandler.kafka;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import uk.gov.companieshouse.kafka.consumer.resilience.CHConsumerType;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.serialization.AvroSerializer;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.orders.OrderReceived;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.APPLICATION_NAMESPACE;

@DirtiesContext
@Aspect
@Service
//TODO: Replace with MockServer/WireMock - shouldn't be altering implementation logic
public class OrderMessageConsumerWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);
    private CountDownLatch latch = new CountDownLatch(1);
    private String orderUri;
    private CHConsumerType testType = CHConsumerType.MAIN_CONSUMER;
    @Value("${spring.kafka.bootstrap-servers}")
    private String brokerAddresses;
    private static final String ORDER_RECEIVED_TOPIC = "order-received";
    private static final String ORDER_RECEIVED_TOPIC_RETRY = "order-received-retry";
    private static final String ORDER_RECEIVED_TOPIC_ERROR = "order-received-error";
    private static final String ORDER_RECEIVED_URI = "/order/ORDER-12345";
    private static final String ORDER_RECEIVED_KEY_RETRY = ORDER_RECEIVED_TOPIC_RETRY;
    @Autowired
    @Qualifier("defaultMessageProducer")
    private MessageProducer messageProducer;
    @Autowired
    private OrderMessageConsumer ordersKafkaConsumer;
    @Autowired
    private SerializerFactory serializerFactory;

    /**
     * mock message processing failure scenario for main listener
     * so that the message can be published to alternate topics '-retry' and '-error'
     * @param message
     * @throws Exception
     */
    @Before(value = "execution(* uk.gov.companieshouse.itemhandler.kafka.OrderMessageConsumer.processOrderReceived(..)) && args(message)")
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
    @AfterThrowing(pointcut = "execution(* uk.gov.companieshouse.itemhandler.kafka.OrderMessageConsumer.*(..))", throwing = "x")
    public void orderProcessedException(final Exception x) throws Throwable {
        LOGGER.info("OrdersKafkaConsumer.processOrderReceived() @AfterThrowing triggered");

        setUpTestKafkaOrdersProducerAndSendMessageToTopic();
    }

    /**
     * emulates receiving of message from kafka topics
     * @param message
     */
    @After(value = "execution(* uk.gov.companieshouse.itemhandler.kafka.OrderMessageConsumer.*(..)) && args(message)")
    public void afterOrderProcessed(final Message message){
        LOGGER.info("OrdersKafkaConsumer.processOrderReceivedRetry() @After triggered");
        this.orderUri = "" + message.getPayload();
        latch.countDown();
    }

    CountDownLatch getLatch() { return latch; }
    String getOrderUri() { return orderUri; }
    void setTestType(CHConsumerType type) { this.testType = type;}

    private void setUpTestKafkaOrdersProducerAndSendMessageToTopic()
            throws ExecutionException, InterruptedException, SerializationException {

        if (this.testType == CHConsumerType.MAIN_CONSUMER) {
            messageProducer.sendMessage(createMessage(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC));
        } else if (this.testType == CHConsumerType.RETRY_CONSUMER) {
            messageProducer.sendMessage(createMessage(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC_RETRY));
        } else if (this.testType == CHConsumerType.ERROR_CONSUMER) {
            messageProducer.sendMessage(createMessage(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC_ERROR));
        }
    }

    //TODO: produces a stub object; shouldn't be declared in consumer wrapper
    public uk.gov.companieshouse.kafka.message.Message createMessage(String orderUri, String topic) throws SerializationException {
        final uk.gov.companieshouse.kafka.message.Message message = new uk.gov.companieshouse.kafka.message.Message();
        AvroSerializer serializer = serializerFactory.getGenericRecordSerializer(OrderReceived.class);
        OrderReceived orderReceived = new OrderReceived();
        orderReceived.setOrderUri(orderUri.trim());

        message.setKey(ORDER_RECEIVED_KEY_RETRY);
        message.setValue(serializer.toBinary(orderReceived));
        message.setTopic(topic);
        message.setTimestamp(new Date().getTime());

        return message;
    }
}
