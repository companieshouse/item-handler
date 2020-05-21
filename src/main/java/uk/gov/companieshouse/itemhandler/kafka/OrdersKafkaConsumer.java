package uk.gov.companieshouse.itemhandler.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.exception.ServiceException;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.serialization.AvroSerializer;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.orders.OrderReceived;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static uk.gov.companieshouse.itemhandler.ItemHandlerApplication.APPLICATION_NAMESPACE;

@Service
public class OrdersKafkaConsumer implements ConsumerSeekAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);
    private static final String ORDER_RECEIVED_TOPIC = "order-received";
    private static final String ORDER_RECEIVED_TOPIC_RETRY = "order-received-retry";
    private static final String ORDER_RECEIVED_KEY_RETRY = ORDER_RECEIVED_TOPIC_RETRY;
    private static final String ORDER_RECEIVED_TOPIC_ERROR = "order-received-error";
    private static final String ORDER_RECEIVED_GROUP = APPLICATION_NAMESPACE + "-" + ORDER_RECEIVED_TOPIC;
    private static final String ORDER_RECEIVED_GROUP_RETRY = APPLICATION_NAMESPACE + "-" + ORDER_RECEIVED_TOPIC_RETRY;
    private static final String ORDER_RECEIVED_GROUP_ERROR = APPLICATION_NAMESPACE + "-" + ORDER_RECEIVED_TOPIC_ERROR;
    private static long ERROR_RECOVERY_OFFSET = 0l;

    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String bootstrapServers;
    @Value("${uk.gov.companieshouse.item-handler.error-consumer}")
    private boolean errorConsumerEnabled;
    private final SerializerFactory serializerFactory;
    private final OrdersKafkaProducer kafkaProducer;
    private final KafkaListenerEndpointRegistry registry;

    public OrdersKafkaConsumer(SerializerFactory serializerFactory,
                               OrdersKafkaProducer kafkaProducer,
                               KafkaListenerEndpointRegistry registry) {
        this.serializerFactory = serializerFactory;
        this.kafkaProducer = kafkaProducer;
        this.registry = registry;
    }

    /**
     * `order-received` topic listener/consumer. Calls a `retryable` method to process received message.
     * If the `retryable` processor is unsuccessful with a `retryable` error, after maximum numbers of attempts allowed,
     * the message is published to `order-received-retry` topic for failover processing.
     * @param message
     * @throws SerializationException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @KafkaListener(id = ORDER_RECEIVED_GROUP, groupId = ORDER_RECEIVED_GROUP,
                    topics = ORDER_RECEIVED_TOPIC,
                    autoStartup = "#{!${uk.gov.companieshouse.item-handler.error-consumer}}")
    public void processOrderReceived(org.springframework.messaging.Message<OrderReceived> message)
            throws SerializationException, ExecutionException, InterruptedException {
        OrderReceived orderReceived = message.getPayload();
        String orderReceivedUri = orderReceived.getOrderUri();
        try {
            logMessageReceived(message, ORDER_RECEIVED_TOPIC);

            logMessageProcessed(message, ORDER_RECEIVED_TOPIC);
        } catch (ServiceException ex){
            logMessageProcessingFailureRecoverable(message, ORDER_RECEIVED_TOPIC, ORDER_RECEIVED_TOPIC_RETRY, ex);
            republishMessageToTopic(orderReceivedUri, ORDER_RECEIVED_TOPIC, ORDER_RECEIVED_TOPIC_RETRY);
        } catch (Exception x) {
            logMessageProcessingFailureNonRecoverable(message, ORDER_RECEIVED_TOPIC, ORDER_RECEIVED_TOPIC_RETRY, x);
        }
    }

    /**
     * `order-received-retry` topic listener/consumer. Calls a `retryable` method to process received message.
     * If the `retryable` processor is unsuccessful with a `retryable` error, after maximum numbers of attempts allowed,
     * the message is published to `-error` topic for failover processing.
     * @param message
     * @throws SerializationException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @KafkaListener(id = ORDER_RECEIVED_GROUP_RETRY, groupId = ORDER_RECEIVED_GROUP_RETRY,
                    topics = ORDER_RECEIVED_TOPIC_RETRY,
                    autoStartup = "#{!${uk.gov.companieshouse.item-handler.error-consumer}}")
    public void processOrderReceivedRetry(org.springframework.messaging.Message<OrderReceived> message)
            throws InterruptedException, ExecutionException, SerializationException {
        OrderReceived orderReceived = message.getPayload();
        String orderReceivedUri = orderReceived.getOrderUri();
        try {
            logMessageReceived(message, ORDER_RECEIVED_TOPIC_RETRY);

            logMessageProcessed(message, ORDER_RECEIVED_TOPIC_RETRY);
        } catch (ServiceException ex){
            logMessageProcessingFailureRecoverable(message, ORDER_RECEIVED_TOPIC_RETRY, ORDER_RECEIVED_TOPIC_ERROR, ex);
            republishMessageToTopic(orderReceivedUri, ORDER_RECEIVED_TOPIC_RETRY, ORDER_RECEIVED_TOPIC_ERROR);
        } catch (Exception x) {
            logMessageProcessingFailureNonRecoverable(message, ORDER_RECEIVED_TOPIC_RETRY, ORDER_RECEIVED_TOPIC_ERROR, x);
        }
    }

    /**
     * `order-received-error` topic listener/consumer is enabled when the application is launched in error
     * mode (IS_ERROR_QUEUE_CONSUMER=true). Receives messages up to `ERROR_RECOVERY_OFFSET` offset. Calls a `retryable`
     * method to process received message. If the `retryable` processor is unsuccessful with a `retryable` error, after
     * maximum numbers of attempts allowed, the message is republished to `-retry` topic for failover processing.
     * This listener stops accepting messages when the topic's offset reaches `ERROR_RECOVERY_OFFSET`.
     * @param message
     * @throws SerializationException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @KafkaListener(id = ORDER_RECEIVED_GROUP_ERROR, groupId = ORDER_RECEIVED_GROUP_ERROR,
                    topics = ORDER_RECEIVED_TOPIC_ERROR,
                    autoStartup = "${uk.gov.companieshouse.item-handler.error-consumer}")
    public void processOrderReceivedError(org.springframework.messaging.Message<OrderReceived> message)
            throws SerializationException, ExecutionException, InterruptedException {
        OrderReceived orderReceived = message.getPayload();
        String orderReceivedUri = orderReceived.getOrderUri();
        try {
            logMessageReceived(message, ORDER_RECEIVED_TOPIC_ERROR);

            logMessageProcessed(message, ORDER_RECEIVED_TOPIC_ERROR);
        } catch (ServiceException ex){
            logMessageProcessingFailureRecoverable(message, ORDER_RECEIVED_TOPIC_ERROR, ORDER_RECEIVED_TOPIC_RETRY, ex);
            republishMessageToTopic(orderReceivedUri, ORDER_RECEIVED_TOPIC_ERROR, ORDER_RECEIVED_TOPIC_RETRY);
        } catch (Exception x) {
            logMessageProcessingFailureNonRecoverable(message, ORDER_RECEIVED_TOPIC_ERROR, ORDER_RECEIVED_TOPIC_RETRY, x);
        } finally {
            long offset = Long.parseLong("" + message.getHeaders().get("kafka_offset"));
            if (offset >= ERROR_RECOVERY_OFFSET) {
                LOGGER.info(String.format("Pausing error consumer \"%1$s\" as error recovery offset '%2$d' reached.",
                        ORDER_RECEIVED_GROUP_ERROR, ERROR_RECOVERY_OFFSET));
                registry.getListenerContainer(ORDER_RECEIVED_GROUP_ERROR).pause();
            }
        }
    }

    private void logMessageReceived(org.springframework.messaging.Message<OrderReceived> message, String topic){
        OrderReceived msg = message.getPayload();
        LOGGER.info(String.format("'order-received' message: [orderUri: \"%1$s\", %2$s] received from topic: \"%3$s\".",
                msg.getOrderUri(), getMessageHeadersAsString(message), topic));
    }

    private void logMessageProcessingFailureRecoverable(org.springframework.messaging.Message<OrderReceived> message,
                                                        String currentTopic, String nextTopic, Exception exception) {
        OrderReceived msg = message.getPayload();
        LOGGER.error(String.format("'order-received' message: [orderUri: \"%1$s\", %2$s] from topic: \"%3$s\" " +
                        "has failed processing. Publishing to topic \"%4$s\" for recovery. Recoverable exception is - \n%4$s",
                msg.getOrderUri(), getMessageHeadersAsString(message), currentTopic, nextTopic, exception.getStackTrace()));
    }

    private void logMessageProcessingFailureNonRecoverable(org.springframework.messaging.Message<OrderReceived> message,
                                                           String currentTopic, String nextTopic, Exception exception) {
        OrderReceived msg = message.getPayload();
        LOGGER.error(String.format("order-received message: [orderUri: \"%1$s\", %2$s] from topic: \"%3$s\" " +
                        "has failed processing with a non-recoverable exception is - \n%4$s",
                msg.getOrderUri(), getMessageHeadersAsString(message), currentTopic, nextTopic, exception.getStackTrace()));
    }

    private String getMessageHeadersAsString(org.springframework.messaging.Message<OrderReceived> message){
        MessageHeaders messageHeaders = message.getHeaders();
        String offset = "" + messageHeaders.get("kafka_offset");
        String key = "" + messageHeaders.get("kafka_receivedMessageKey");
        String partition = "" + messageHeaders.get("kafka_receivedPartitionId");

        return String.format("key: \"%1$s\", offset: %2$s, partition: %3$s", key, offset, partition);
    }

    private void logMessageProcessed(org.springframework.messaging.Message<OrderReceived> message, String topic){
        OrderReceived msg = message.getPayload();
        LOGGER.info(String.format("Order received message: [orderReceivedUri: \"%1$s\", %2$s] received from topic: \"%3$s\" " +
                        "successfully processed.",
                msg.getOrderUri(), getMessageHeadersAsString(message), topic));
    }

    protected void republishMessageToTopic(String orderUri, String currentTopic, String nextTopic)
            throws SerializationException, ExecutionException, InterruptedException {
        LOGGER.info(String.format("Republishing message: \"%1$s\" received from topic: \"%2$s\" to topic: \"%3$s\"",
                orderUri, currentTopic, nextTopic));
        Message message = createRetryMessage(orderUri, nextTopic);
        kafkaProducer.sendMessage(message);
    }

    protected Message createRetryMessage(String orderUri, String topic) throws SerializationException {
        final Message message = new Message();
        AvroSerializer serializer = serializerFactory.getGenericRecordSerializer(OrderReceived.class);
        OrderReceived orderReceived = new OrderReceived();
        orderReceived.setOrderUri(orderUri.trim());

        message.setKey(ORDER_RECEIVED_KEY_RETRY);
        message.setValue(serializer.toBinary(orderReceived));
        message.setTopic(topic);
        message.setTimestamp(new Date().getTime());

        return message;
    }

    /**
     * Sets ERROR_RECOVERY_OFFSET to last read message offset (error topic) minus 1, before error consumer starts. This
     * helps the error consumer to stop consuming messages when all messages up to ERROR_RECOVERY_OFFSET are processed.
     * @param map map of topics and partitions
     * @param consumerSeekCallback callback that allows a consumers offset position to be moved.
     */
    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> map, ConsumerSeekCallback consumerSeekCallback) {
        if (errorConsumerEnabled) {
            try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(errorConsumerConfigs())) {
                final Map<TopicPartition, Long> topicPartitionsMap = consumer.endOffsets(map.keySet());
                map.forEach(
                        (topic, action) ->
                        {
                            ERROR_RECOVERY_OFFSET = topicPartitionsMap.get(topic) - 1;
                            consumerSeekCallback.seek(topic.topic(), topic.partition(), ERROR_RECOVERY_OFFSET);
                            LOGGER.info(String.format("Setting Error Consumer Recovery Offset to '%1$d'", ERROR_RECOVERY_OFFSET));
                        }
                );
            }
        }
    }

    @Override
    public void registerSeekCallback(ConsumerSeekCallback consumerSeekCallback) {}

    @Override
    public void onIdleContainer(Map<TopicPartition, Long> map, ConsumerSeekCallback consumerSeekCallback) {}

    private Map errorConsumerConfigs() {
        Map props = new HashMap();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, ORDER_RECEIVED_GROUP_ERROR);

        return props;
    }
}
