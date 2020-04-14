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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static uk.gov.companieshouse.itemhandler.ItemHandlerApplication.APPLICATION_NAMESPACE;

@Service
public class OrdersKafkaConsumer implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);
    private static final String ORDER_RECEIVED_TOPIC = "order-received";
    private static final String ORDER_RECEIVED_TOPIC_RETRY = "order-received-retry";
    private static final String ORDER_RECEIVED_TOPIC_ERROR = "order-received-error";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final String GROUP_NAME = "order-received-consumers";
    private CHKafkaResilientConsumerGroup chKafkaConsumerGroupMain;
    private CHKafkaResilientConsumerGroup chKafkaConsumerGroupRetry;
    private CHKafkaResilientConsumerGroup chKafkaConsumerGroupError;
    @Value("${kafka.broker.addresses}")
    private String brokerAddresses;
    private CHKafkaProducer chKafkaProducer;
    private final SerializerFactory serializerFactory;

    public OrdersKafkaConsumer(SerializerFactory serializerFactory) {
        this.serializerFactory = serializerFactory;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        LOGGER.debug("Initializing kafka consumer service " + this.toString());

        ProducerConfig config = getProducerConfig();
        chKafkaProducer = new CHKafkaProducer(config);

        ConsumerConfig consumerConfig = getConsumerConfig();

        chKafkaConsumerGroupMain = new CHKafkaResilientConsumerGroup(consumerConfig, CHConsumerType.MAIN_CONSUMER,
                new KafkaConsumerFactory(), chKafkaProducer);
        chKafkaConsumerGroupRetry = new CHKafkaResilientConsumerGroup(consumerConfig, CHConsumerType.RETRY_CONSUMER,
                new KafkaConsumerFactory(), chKafkaProducer);
    }

    @KafkaListener(topics = ORDER_RECEIVED_TOPIC, groupId = GROUP_NAME)
    public void processOrderReceived(String orderReceivedUri)
            throws SerializationException, ExecutionException, InterruptedException {
        try {
            LOGGER.info("Message: " + orderReceivedUri + " received on topic: " + ORDER_RECEIVED_TOPIC);
        } catch (Exception x){
            Message message = createRetryMessage(orderReceivedUri);
            for (int attempt = 0; attempt < MAX_RETRY_ATTEMPTS; attempt++) {
                chKafkaConsumerGroupMain.retry(attempt, message);
            }
        }
    }

    @KafkaListener(topics = ORDER_RECEIVED_TOPIC_RETRY, groupId = GROUP_NAME)
    public void processOrderReceivedRetry(String orderReceivedUri)
            throws SerializationException, ExecutionException, InterruptedException {
        try {
            LOGGER.info("Message: " + orderReceivedUri + " received on topic: " + ORDER_RECEIVED_TOPIC_RETRY);
        } catch (Exception x){
            Message message = createRetryMessage(orderReceivedUri);
            for (int attempt = 0; attempt < MAX_RETRY_ATTEMPTS; attempt++) {
                chKafkaConsumerGroupRetry.retry(attempt, message);
            }
        }
    }

    private ConsumerConfig getConsumerConfig() {
        ConsumerConfig consumerConfig = new ConsumerConfig();
        List<String> topics = new ArrayList<>();
        topics.add(ORDER_RECEIVED_TOPIC);
        topics.add(ORDER_RECEIVED_TOPIC_RETRY);
        topics.add(ORDER_RECEIVED_TOPIC_ERROR);
        consumerConfig.setTopics(topics);
        consumerConfig.setMaxRetries(MAX_RETRY_ATTEMPTS);
        consumerConfig.setKeyDeserializer(StringDeserializer.class.getName());
        consumerConfig.setValueDeserializer(StringDeserializer.class.getName());
        consumerConfig.setGroupName(GROUP_NAME);
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

    private Message createRetryMessage(String orderUri) throws SerializationException {
        final Message message = new Message();
        AvroSerializer serializer = serializerFactory.getGenericRecordSerializer(OrderReceived.class);
        OrderReceived orderReceived = new OrderReceived();
        orderReceived.setOrderUri(orderUri);

        message.setValue(serializer.toBinary(orderReceived));
        message.setTopic(ORDER_RECEIVED_TOPIC);
        message.setTimestamp(new Date().getTime());

        return message;
    }
}
