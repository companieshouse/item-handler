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
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

@Aspect
@Service
public class OrdersKafkaConsumerWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrdersKafkaConsumerWrapper.class.getName());
    private CountDownLatch latch = new CountDownLatch(1);
    private String orderUri;
    private String testType = "DEFAULT";
    @Value("${kafka.broker.addresses}")
    private String brokerAddresses;
    private KafkaTemplate<String, String> template;
    public static final String ORDER_RECEIVED_RETRY_TOPIC = "order-received-retry";
    private static final String ORDER_RECEIVED_URI = "/order/ORDER-12345";

    @Before(value = "execution(* uk.gov.companieshouse.itemhandler.kafka.OrdersKafkaConsumer.processOrderReceived(..)) && args(message)")
    public void beforeOrderProcessed(final String message) throws Exception {
        LOGGER.info("OrdersKafkaConsumer.processOrderReceived() @Before triggered");
        if (this.testType.equals("RETRY")) {
            throw new Exception("Mock main listener exception");
        }
    }

    @AfterThrowing(pointcut = "execution(* uk.gov.companieshouse.itemhandler.kafka.OrdersKafkaConsumer.*(..))", throwing = "x")
    public void orderProcessedException(final Exception x) throws Throwable {
        LOGGER.info("OrdersKafkaConsumer.processOrderReceived() @AfterThrowing triggered");
        setUpTestKafkaOrdersProducerAndSendMessageToRetryTopic();
    }

    @After(value = "execution(* uk.gov.companieshouse.itemhandler.kafka.OrdersKafkaConsumer.*(..)) && args(message)")
    public void afterOrderProcessed(final String message){
        LOGGER.info("OrdersKafkaConsumer.processOrderReceivedRetry() @After triggered");
        latch.countDown();
        this.orderUri = message;
    }

    CountDownLatch getLatch() { return latch; }
    String getOrderUri() { return orderUri; }
    void setTestType(String type) { this.testType = type;}

    private void setUpTestKafkaOrdersProducerAndSendMessageToRetryTopic() {
        final Map<String, Object> senderProperties = KafkaTestUtils.senderProps(brokerAddresses);

        senderProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        final ProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(senderProperties);

        template = new KafkaTemplate<>(producerFactory);
        template.setDefaultTopic(ORDER_RECEIVED_RETRY_TOPIC);

        template.send(ORDER_RECEIVED_RETRY_TOPIC, ORDER_RECEIVED_URI);
    }
}
