package uk.gov.companieshouse.itemhandler.config;

import email.email_send;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import uk.gov.companieshouse.itemhandler.kafka.MessageDeserialiser;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.orders.OrderReceived;
import uk.gov.companieshouse.orders.items.ChdItemOrdered;

@TestConfiguration
public class EmbeddedKafkaBrokerConfiguration {
    @Bean
    EmbeddedKafkaBroker embeddedKafkaBroker() {
        return new EmbeddedKafkaBroker(1);
    }

    @Bean
    @Scope("prototype")
    KafkaConsumer<String, email_send> emailSendConsumer(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // TODO: remove props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, MessageDeserialiser.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "" + new Random().nextInt());
        return new KafkaConsumer<>(props,
                new StringDeserializer(),
                new MessageDeserialiser<>(email_send.class));
    }

    @Bean
    @Scope("prototype")
    KafkaConsumer<String, OrderReceived> orderReceivedConsumer(@Value("${spring.kafka"
            + ".bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // TODO: remove props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, MessageDeserialiser.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "" + new Random().nextInt());
        return new KafkaConsumer<>(props,
                new StringDeserializer(),
                new MessageDeserialiser<>(OrderReceived.class));
    }

    @Bean
    @Scope("prototype")
    KafkaConsumer<String, ChdItemOrdered> chdItemOrderedConsumer(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // TODO: remove props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, MessageDeserialiser.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "" + new Random().nextInt());
        return new KafkaConsumer<>(props,
                new StringDeserializer(),
                new MessageDeserialiser<>(ChdItemOrdered.class));
    }

    @Bean
    KafkaProducer<String, OrderReceived> myProducer(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        return createProducer(bootstrapServers, OrderReceived.class);
    }

    @Bean
    KafkaProducer<String, String> serializableKafkaProducer(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaProducer<>(config, new StringSerializer(), new StringSerializer());
    }

    private <T extends SpecificRecord> KafkaProducer<String, T> createProducer(String bootstrapServers, Class<T> type) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaProducer<>(config, new StringSerializer(), (topic, data) -> {
            try {
                return new SerializerFactory().getSpecificRecordSerializer(type)
                        .toBinary(data); //creates a leading space
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
