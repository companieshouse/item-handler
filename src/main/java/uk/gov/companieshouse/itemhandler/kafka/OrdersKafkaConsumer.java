package uk.gov.companieshouse.itemhandler.kafka;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.kafka.consumer.ConsumerConfig;
import uk.gov.companieshouse.kafka.consumer.factory.KafkaConsumerFactory;
import uk.gov.companieshouse.kafka.consumer.resilience.CHConsumerType;
import uk.gov.companieshouse.kafka.consumer.resilience.CHKafkaResilientConsumerGroup;
import uk.gov.companieshouse.kafka.exceptions.ProducerConfigException;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.producer.Acks;
import uk.gov.companieshouse.kafka.producer.CHKafkaProducer;
import uk.gov.companieshouse.kafka.producer.ProducerConfig;
import uk.gov.companieshouse.kafka.serialization.AvroSerializer;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.orders.OrderReceived;

import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import static uk.gov.companieshouse.itemhandler.ItemHandlerApplication.APPLICATION_NAMESPACE;

@Service
public class OrdersKafkaConsumer implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);
    private static final String ORDER_RECEIVED_TOPIC = "order-received";
    private static final String ORDER_RECEIVED_TOPIC_RETRY = "order-received-retry";
    private static final String ORDER_RECEIVED_TOPIC_ERROR = "order-received-error";
    private static final String ORDER_RECEIVED_GROUP = APPLICATION_NAMESPACE + "-" + ORDER_RECEIVED_TOPIC;
    private static final String ORDER_RECEIVED_GROUP_RETRY = APPLICATION_NAMESPACE + "-" + ORDER_RECEIVED_TOPIC_RETRY;
    private static final String ORDER_RECEIVED_GROUP_ERROR = APPLICATION_NAMESPACE + "-" + ORDER_RECEIVED_TOPIC_ERROR;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private CHKafkaResilientConsumerGroup chKafkaConsumerGroupMain;
    private CHKafkaResilientConsumerGroup chKafkaConsumerGroupRetry;
    @Value("${kafka.broker.addresses}")
    private String brokerAddresses;
    private final SerializerFactory serializerFactory;

    public OrdersKafkaConsumer(SerializerFactory serializerFactory) {
        this.serializerFactory = serializerFactory;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        LOGGER.debug("Initializing kafka consumer service " + this.toString());

        ProducerConfig config = getProducerConfig();
        CHKafkaProducer chKafkaProducer = new CHKafkaProducer(config);

        ConsumerConfig consumerConfig = getConsumerConfig();

        consumerConfig.setGroupName(ORDER_RECEIVED_GROUP);
        consumerConfig.setTopics(Collections.singletonList(ORDER_RECEIVED_TOPIC));
        chKafkaConsumerGroupMain
                = new CHKafkaResilientConsumerGroup(consumerConfig, CHConsumerType.MAIN_CONSUMER,
                        new KafkaConsumerFactory(), chKafkaProducer);

        consumerConfig.setTopics(Collections.singletonList(ORDER_RECEIVED_TOPIC_RETRY));
        consumerConfig.setGroupName(ORDER_RECEIVED_GROUP_RETRY);
        chKafkaConsumerGroupRetry
                = new CHKafkaResilientConsumerGroup(consumerConfig, CHConsumerType.RETRY_CONSUMER,
                        new KafkaConsumerFactory(), chKafkaProducer);
    }

    @KafkaListener(topics = ORDER_RECEIVED_TOPIC, groupId = ORDER_RECEIVED_GROUP,
            autoStartup = "#{!${uk.gov.companieshouse.item-handler.error-consumer}}")
    public void processOrderReceived(String orderReceivedUri)
            throws SerializationException, ExecutionException, InterruptedException {
        try {
            logMessageReceived(orderReceivedUri, ORDER_RECEIVED_TOPIC);
        } catch (Exception x){
            republishMessageToTopic(orderReceivedUri, ORDER_RECEIVED_TOPIC, ORDER_RECEIVED_TOPIC_RETRY, x.getMessage());
        }
    }

    @KafkaListener(topics = ORDER_RECEIVED_TOPIC_RETRY, groupId = ORDER_RECEIVED_GROUP_RETRY,
            autoStartup = "#{!${uk.gov.companieshouse.item-handler.error-consumer}}")
    public void processOrderReceivedRetry(String orderReceivedUri)
            throws SerializationException, ExecutionException, InterruptedException {
        try {
            logMessageReceived(orderReceivedUri, ORDER_RECEIVED_TOPIC_RETRY);
        } catch (Exception x){
            republishMessageToTopic(orderReceivedUri, ORDER_RECEIVED_TOPIC_RETRY, ORDER_RECEIVED_TOPIC_ERROR, x.getMessage());
        }
    }

    @KafkaListener(topics = ORDER_RECEIVED_TOPIC_ERROR, groupId = ORDER_RECEIVED_GROUP_ERROR,
            autoStartup = "${uk.gov.companieshouse.item-handler.error-consumer}")
    public void processOrderReceivedError(String orderReceivedUri) {
        try {
            logMessageReceived(orderReceivedUri, ORDER_RECEIVED_TOPIC_ERROR);
        } catch (Exception x){
            LOGGER.error("Message: " + orderReceivedUri + " received on topic: " + ORDER_RECEIVED_TOPIC_ERROR + " could not be processed.");
        }
    }

    private ConsumerConfig getConsumerConfig() {
        ConsumerConfig consumerConfig = new ConsumerConfig();
        consumerConfig.setMaxRetries(MAX_RETRY_ATTEMPTS);
        consumerConfig.setKeyDeserializer(StringDeserializer.class.getName());
        consumerConfig.setValueDeserializer(StringDeserializer.class.getName());
        consumerConfig.setResetOffset(false);
        consumerConfig.setBrokerAddresses(brokerAddresses.split(","));
        consumerConfig.setAutoCommit(true);
        return consumerConfig;
    }

    private ProducerConfig getProducerConfig() {
        ProducerConfig config = new ProducerConfig();
        if (brokerAddresses != null && !brokerAddresses.isEmpty()) {
            config.setBrokerAddresses(brokerAddresses.split(","));
        } else {
            throw new ProducerConfigException("Broker addresses for kafka broker missing, check if environment variable KAFKA_BROKER_ADDR is configured. " +
                    "[Hint: The property 'kafka.broker.addresses' uses the value of this environment variable in live environments " +
                    "and that of 'spring.embedded.kafka.brokers' property in test.]");
        }

        config.setRoundRobinPartitioner(true);
        config.setAcks(Acks.WAIT_FOR_ALL);
        config.setRetries(10);
        return config;
    }

    protected void republishMessageToTopic(String orderUri, String currentTopic, String nextTopic, String errorMessage)
            throws SerializationException, ExecutionException, InterruptedException {
        LOGGER.error("Processing message: " + orderUri + " received on topic: " + currentTopic
                + " failed with exception: " + errorMessage);
        Message message = createRetryMessage(orderUri);
        LOGGER.info("Republishing message: " + orderUri + " received on topic: " + currentTopic
                + " to topic: " + nextTopic);
        for (int attempt = 0; attempt < MAX_RETRY_ATTEMPTS; attempt++) {
            if (currentTopic.equals(ORDER_RECEIVED_TOPIC)) {
                chKafkaConsumerGroupMain.retry(attempt, message);
            }
            else if (currentTopic.equals(ORDER_RECEIVED_TOPIC_RETRY)) {
                chKafkaConsumerGroupRetry.retry(attempt, message);
            }
        }
    }

    protected Message createRetryMessage(String orderUri) throws SerializationException {
        final Message message = new Message();
        AvroSerializer serializer = serializerFactory.getGenericRecordSerializer(OrderReceived.class);
        OrderReceived orderReceived = new OrderReceived();
        orderReceived.setOrderUri(orderUri);

        message.setValue(serializer.toBinary(orderReceived));
        message.setTopic(ORDER_RECEIVED_TOPIC_RETRY);
        message.setTimestamp(new Date().getTime());

        return message;
    }

    private void logMessageReceived(String message, String topic){
        LOGGER.info(String.format("Message: %1$s received on topic: %2$s", message, topic));
    }

    public void setChKafkaConsumerGroupMain(CHKafkaResilientConsumerGroup chKafkaConsumerGroupMain) {
        this.chKafkaConsumerGroupMain = chKafkaConsumerGroupMain;
    }

    public void setChKafkaConsumerGroupRetry(CHKafkaResilientConsumerGroup chKafkaConsumerGroupRetry) {
        this.chKafkaConsumerGroupRetry = chKafkaConsumerGroupRetry;
    }
}
