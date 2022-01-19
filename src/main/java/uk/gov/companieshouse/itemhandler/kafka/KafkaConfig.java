package uk.gov.companieshouse.itemhandler.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.SeekToCurrentErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import uk.gov.companieshouse.email.EmailSend;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.kafka.producer.Acks;
import uk.gov.companieshouse.kafka.producer.CHKafkaProducer;
import uk.gov.companieshouse.kafka.producer.ProducerConfig;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.orders.OrderReceived;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${duplicate-message-cache-size}")
    private int duplicateMessageCacheSize;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderReceived> kafkaListenerContainerFactory() {
        return getContainerFactory(getDefaultConsumerProperties());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderReceived> kafkaListenerContainerFactoryError() {
        final Map<String, Object> props = getDefaultConsumerProperties();
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return getContainerFactory(props);
    }

    @Bean
    CHKafkaProducer defaultKafkaProducer(ProducerConfig defaultProducerConfig) {
        return new CHKafkaProducer(defaultProducerConfig);
    }

    @Bean
    CHKafkaProducer chdKafkaProducer(ProducerConfig chdProducerConfig) {
        return new CHKafkaProducer(chdProducerConfig);
    }

    @Bean
    @Scope("prototype")
    ProducerConfig defaultProducerConfig() {
        ProducerConfig config = new ProducerConfig();
        config.setBrokerAddresses(bootstrapServers.split(","));
        config.setRoundRobinPartitioner(true);
        config.setAcks(Acks.WAIT_FOR_ALL);
        config.setRetries(10);
        return config;
    }

    @Bean
    ProducerConfig chdProducerConfig() {
        ProducerConfig config = defaultProducerConfig();
        config.setMaxBlockMilliseconds(10000);
        return config;
    }

    @Bean
    MessageProducer defaultMessageProducer(CHKafkaProducer defaultKafkaProducer, Logger logger) {
        return new MessageProducer(defaultKafkaProducer, logger);
    }

    @Bean
    MessageProducer chdMessageProducer(CHKafkaProducer chdKafkaProducer, Logger logger) {
        return new MessageProducer(chdKafkaProducer, logger);
    }

    @Bean
    OrderMessageProducer orderMessageProducer(MessageSerialiserFactory<OrderReceived> orderReceivedMessageSerialiserFactory, MessageProducer defaultMessageProducer, Logger logger) {
        return new OrderMessageProducer(orderReceivedMessageSerialiserFactory, defaultMessageProducer, logger);
    }

    @Bean
    ItemMessageProducer itemMessageProducer(ItemMessageFactory itemMessageFactory, MessageProducer chdMessageProducer) {
        return new ItemMessageProducer(itemMessageFactory, chdMessageProducer);
    }

    @Bean
    EmailSendMessageProducer emailSendMessageProducer(MessageSerialiserFactory<EmailSend> emailSendMessageSerialiserFactory, MessageProducer defaultMessageProducer) {
        return new EmailSendMessageProducer(emailSendMessageSerialiserFactory, defaultMessageProducer);
    }

    @Bean
    MessageSerialiserFactory<EmailSend> emailSendMessageSerialiserFactory(SerializerFactory serializerFactory) {
        return new MessageSerialiserFactory<>(serializerFactory, EmailSend.class);
    }

    @Bean
    MessageSerialiserFactory<OrderReceived> orderReceivedMessageSerialiserFactory(SerializerFactory serializerFactory) {
        return new MessageSerialiserFactory<>(serializerFactory, OrderReceived.class);
    }

    @Bean
    @ConfigurationProperties(prefix = "kafka.topics")
    KafkaTopics kafkaTopics() {
        return new KafkaTopics();
    }

    @Bean
    PartitionOffset errorRecoveryOffset() {
        return new PartitionOffset();
    }

    @Bean
    @Scope("prototype")
    MessageFilter<OrderReceived> duplicateMessageFilter(Logger logger) {
        logger.info("DuplicateMessageFilter config",
                LoggingUtils.logIfNotNull(LoggingUtils.createLogMap(),"duplicate-message-cache-size", duplicateMessageCacheSize));
        return new DuplicateMessageFilter(duplicateMessageCacheSize, logger);
    }

    private ConcurrentKafkaListenerContainerFactory<String, OrderReceived> getContainerFactory(Map<String, Object> props) {
        ConcurrentKafkaListenerContainerFactory<String, OrderReceived> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new OrderReceivedDeserialiser()));
        factory.setErrorHandler(new SeekToCurrentErrorHandler(new FixedBackOff(0, 0)));
        return factory;
    }

    private Map<String, Object> getDefaultConsumerProperties() {
        final Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, Boolean.toString(false));
        return props;
    }
}