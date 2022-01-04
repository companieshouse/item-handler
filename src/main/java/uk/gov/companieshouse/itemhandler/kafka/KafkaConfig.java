package uk.gov.companieshouse.itemhandler.kafka;

import java.util.function.Supplier;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.SeekToCurrentErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import uk.gov.companieshouse.email.EmailSend;
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

    @Value("${kafka.topics.order-received-notification-error-group}")
    private String orderReceivedErrorGroup;

    @Bean
    public ConsumerFactory<String, OrderReceived> consumerFactoryMessage() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs(), new StringDeserializer(),
                new MessageDeserialiser<>(OrderReceived.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderReceived> kafkaListenerContainerFactory(ConsumerFactory<String, OrderReceived> consumerFactoryMessage) {
        ConcurrentKafkaListenerContainerFactory<String, OrderReceived> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactoryMessage);
        factory.setErrorHandler(new SeekToCurrentErrorHandler(new FixedBackOff(0, 0)));
        return factory;
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
    Supplier<Map<String, Object>> consumerConfigsErrorSupplier() {
        final Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, OrderReceivedDeserialiser.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, orderReceivedErrorGroup);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return () -> props;
    }

    private Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, MessageDeserialiser.class);

        return props;
    }
}