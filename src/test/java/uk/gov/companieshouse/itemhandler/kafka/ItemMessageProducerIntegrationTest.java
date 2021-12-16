package uk.gov.companieshouse.itemhandler.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.itemhandler.util.TestUtils;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.serialization.AvroSerializer;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.orders.items.ChdItemOrdered;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;
import static uk.gov.companieshouse.itemhandler.util.TestConstants.MISSING_IMAGE_DELIVERY_ITEM_ID;
import static uk.gov.companieshouse.itemhandler.util.TestConstants.ORDER_REFERENCE;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka
class ItemMessageProducerIntegrationTest {

    @Configuration
    @ComponentScan(basePackageClasses = ItemMessageProducerIntegrationTest.class,
                   excludeFilters = {@ComponentScan.Filter(type = ASSIGNABLE_TYPE,
                                                           value = OrderMessageConsumerWrapper.class),
                                     @ComponentScan.Filter(type = ASSIGNABLE_TYPE,
                                                           value = OrderMessageConsumer.class)})
    public static class Config {
        @Bean
        public SerializerFactory getSerializerFactory() {
            return new SerializerFactory();
        }

        @Bean
        public ItemMessageProducer itemMessageProducerUnderTest() {
            return new ItemMessageProducer(getItemMessageFactory(), getOrdersKafkaProducer());
        }

        @Bean
        public ItemMessageFactory getItemMessageFactory() {
            return new ItemMessageFactory(getSerializerFactory(), getObjectMapper());
        }

        @Bean
        public MessageProducer getOrdersKafkaProducer() {
            return new MessageProducer();
        }

        @Bean
        public ObjectMapper getObjectMapper() {
            return new ObjectMapper();
        }

        @Bean
        public ItemMessageFactory getMessageFactory() {
            return new ItemMessageFactory(getSerializerFactory(), getObjectMapper());
        }
    }

    @Autowired
    private ItemMessageProducer itemMessageProducerUnderTest;

    @Autowired
    private TestItemMessageConsumer testItemMessageConsumer;

    @Autowired
    private SerializerFactory serializerFactory;

    @Autowired
    private ItemMessageFactory itemMessageFactory;

    @Test
    @DisplayName("sendMessage produces message to the chd-item-ordered Kafka topic")
    void testSendItemMessageToKafkaTopic() throws Exception {

        // Given an item from the order is to be sent
        final OrderData order = TestUtils.createOrder();

        // Given that the actual message produced to the topic is a ChdItemOrdered object.
        final ChdItemOrdered chdItemOrdered = itemMessageFactory.buildChdItemOrdered(order);

        // When ChdItemOrdered message is sent to kafka topic
        final List<Message> messages =
                sendAndConsumeMessage(order, ORDER_REFERENCE, MISSING_IMAGE_DELIVERY_ITEM_ID);

        // Then we have successfully consumed a message.
        assertThat(messages.isEmpty(), is(false));
        final byte[] consumedMessageSerialized = messages.get(0).getValue();
        final String deserializedConsumedMessage = new String(consumedMessageSerialized);

        // And it matches the serialized ChdItemOrdered object
        final AvroSerializer<ChdItemOrdered> serializer =
                serializerFactory.getGenericRecordSerializer(ChdItemOrdered.class);
        final byte[] serializedChdItemOrdered = serializer.toBinary(chdItemOrdered);
        final String deserializedChdItemOrdered = new String(serializedChdItemOrdered);

        assertEquals(deserializedConsumedMessage, deserializedChdItemOrdered);
    }

    private List<Message> sendAndConsumeMessage(final OrderData order,
                                                final String orderReference,
                                                final String itemId) {
        List<Message> messages;
        testItemMessageConsumer.connect();
        int count = 0;
        do {
            messages = testItemMessageConsumer.pollConsumerGroup();
            itemMessageProducerUnderTest.sendMessage(order, orderReference, itemId);
            count++;
        } while (messages.isEmpty() && count < 15);

        return messages;
    }
}
