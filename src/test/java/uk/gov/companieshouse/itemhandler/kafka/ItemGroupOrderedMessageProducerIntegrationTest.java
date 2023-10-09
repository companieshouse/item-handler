package uk.gov.companieshouse.itemhandler.kafka;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import consumer.deserialization.AvroDeserializer;
import email.email_send;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import uk.gov.companieshouse.itemhandler.config.LoggerConfig;
import uk.gov.companieshouse.itemhandler.itemsummary.ItemGroup;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.fasterxml.jackson.databind.PropertyNamingStrategy.SNAKE_CASE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;

/**
 * Integration tests the {@link ItemGroupOrderedMessageProducer}.
 */
@SpringBootTest
@SpringJUnitConfig(classes={
        ItemGroupOrderedMessageProducer.class,
        EmailSendFactory.class,
        ItemGroupOrderedMessageProducerIntegrationTest.Config.class,
        LoggerConfig.class,
        ItemMessageFactory.class
})
@EmbeddedKafka
class ItemGroupOrderedMessageProducerIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger("ItemGroupOrderedMessageProducerIntegrationTest");
    private static final String ITEM_GROUP_ORDERED_TOPIC = "item-group-ordered";
    private static final String ITEM_KIND_CERTIFICATE = "item#certificate";
    private static final String CERTIFICATE_ID = "CRT-110716-962307";
    private static final int MESSAGE_WAIT_TIMEOUT_SECONDS = 10;
    private static final email_send EXPECTED_ITEM_GROUP_ORDERED_MESSAGE = email_send.newBuilder()
            .setAppId("item-handler")
            .setMessageId(CERTIFICATE_ID)
            .setMessageType("TBD")
            .setData("TBD")
            .setCreatedAt("TBD")
            .setEmailAddress("unknown@unknown.com")
            .build();

    @Autowired
    private ItemGroupOrderedMessageProducer producerUnderTest;

    private final CountDownLatch messageReceivedLatch = new CountDownLatch(1);
    private email_send messageReceived;

    @Configuration
    @EnableKafka
    static class Config {

        @Bean
        public ConsumerFactory<String, email_send> consumerFactory(
                @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
            return new DefaultKafkaConsumerFactory<>(
                    new HashMap<String, Object>() {{
                        put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                        put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
                        put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
                        put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
                        put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, AvroDeserializer.class);
                        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
                        put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
                    }},
                    new StringDeserializer(),
                    new ErrorHandlingDeserializer<>(new AvroDeserializer<>(email_send.class)));
        }

        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, email_send> kafkaListenerContainerFactory(
                ConsumerFactory<String, email_send> consumerFactory) {
            ConcurrentKafkaListenerContainerFactory<String, email_send> factory =
                    new ConcurrentKafkaListenerContainerFactory<>();
            factory.setConsumerFactory(consumerFactory);
            factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
            return factory;
        }

        @Bean
        public ProducerFactory<String, email_send> emailSendProducerFactory(
                @Value("${spring.kafka.bootstrap-servers}" ) final String bootstrapServers) {
            final Map<String, Object> config = new HashMap<>();
            config.put(
                    org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                    StringSerializer.class);
            config.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, EmailSendAvroSerializer.class);
            config.put(org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            return new DefaultKafkaProducerFactory<>(config);
        }

        @Bean
        public KafkaTemplate<String, email_send> emailSendKafkaTemplate(
                @Value("${spring.kafka.bootstrap-servers}" ) final String bootstrapServers) {
            return new KafkaTemplate<>(emailSendProducerFactory(bootstrapServers));
        }

        @Bean
        SerializerFactory serializerFactory() {
            return new SerializerFactory();
        }

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper()
                    .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .setPropertyNamingStrategy(SNAKE_CASE)
                    .findAndRegisterModules();
        }

    }

    @Test
    @DisplayName("ItemGroupOrderedMessageProducer produces a message to item-group-ordered successfully")
    void producesMessageSuccessfully() throws InterruptedException {

        producerUnderTest.sendMessage(createDigitalCertificateItemGroup());

        verifyExpectedMessageIsReceived();
    }

    @KafkaListener(topics = ITEM_GROUP_ORDERED_TOPIC, groupId = "test-group")
    public void receiveMessage(final @Payload email_send message) {
        LOGGER.info("Received message: " + message);
        messageReceived = message;
        messageReceivedLatch.countDown();
    }

    private void verifyExpectedMessageIsReceived() throws InterruptedException {
        verifyWhetherMessageIsReceived();
        assertThat(messageReceived, is(notNullValue()));
        assertThat(Objects.deepEquals(messageReceived, EXPECTED_ITEM_GROUP_ORDERED_MESSAGE), is(true));
    }

    private void verifyWhetherMessageIsReceived() throws InterruptedException {
        LOGGER.info("Waiting to receive message for up to " + MESSAGE_WAIT_TIMEOUT_SECONDS + " seconds.");
        final boolean received = messageReceivedLatch.await(MESSAGE_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(received, is(true));
    }

    private static ItemGroup createDigitalCertificateItemGroup() {
        final OrderData order = new OrderData();
        final Item digitalCertificate = new Item();
        digitalCertificate.setId(CERTIFICATE_ID);
        final List<Item> items = Collections.singletonList(digitalCertificate);
        order.setReference("ORD-844016-962315");
        return new ItemGroup(order, ITEM_KIND_CERTIFICATE, items);
    }

}
