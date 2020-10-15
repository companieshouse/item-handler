// TODO GCI-1301 Restore this
//package uk.gov.companieshouse.itemhandler.kafka;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.ComponentScan;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.kafka.test.context.EmbeddedKafka;
//import org.springframework.test.annotation.DirtiesContext;
//import uk.gov.companieshouse.itemhandler.model.Item;
//import uk.gov.companieshouse.kafka.message.Message;
//import uk.gov.companieshouse.kafka.serialization.AvroSerializer;
//import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
//import uk.gov.companieshouse.orders.OrderReceived;
//
//import java.util.List;
//
//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.hamcrest.Matchers.is;
//import static org.junit.Assert.assertEquals;
//import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;
//import static uk.gov.companieshouse.itemhandler.util.TestConstants.MISSING_IMAGE_DELIVERY_ITEM_ID;
//import static uk.gov.companieshouse.itemhandler.util.TestConstants.ORDER_REFERENCE;
//
//@SpringBootTest
//@DirtiesContext
//@EmbeddedKafka
//class ItemMessageProducerIntegrationTest {
//
//    @Configuration
//    @ComponentScan(basePackageClasses = ItemMessageProducerIntegrationTest.class,
//                   excludeFilters = {@ComponentScan.Filter(type = ASSIGNABLE_TYPE,
//                                                           value = OrdersKafkaConsumerWrapper.class),
//                                     @ComponentScan.Filter(type = ASSIGNABLE_TYPE,
//                                                           value = OrdersKafkaConsumer.class)})
//    public static class Config {
//        @Bean
//        public SerializerFactory getSerializerFactory() {
//            return new SerializerFactory();
//        }
//
//        @Bean
//        public ItemMessageProducer itemMessageProducerUnderTest() {
//            return new ItemMessageProducer(getItemMessageFactory(), getItemKafkaProducer());
//        }
//
//        @Bean
//        public ItemMessageFactory getItemMessageFactory() {
//            return new ItemMessageFactory(getSerializerFactory());
//        }
//
//        @Bean
//        public ItemKafkaProducer getItemKafkaProducer() {
//            return new ItemKafkaProducer();
//        }
//    }
//
//    @Autowired
//    private ItemMessageProducer itemMessageProducerUnderTest;
//
//    @Autowired
//    private TestItemMessageConsumer testItemMessageConsumer;
//
//    @Autowired
//    private SerializerFactory serializerFactory;
//
//    @Test
//    void testSendItemMessageToKafkaTopic() throws Exception {
//
//        // Given an item is to be sent
//        final Item item = new Item();
//        item.setId(MISSING_IMAGE_DELIVERY_ITEM_ID);
//
//        // Given that for now the actual message produced to the topic is an OrderReceived object.
//        final OrderReceived orderReceived = new OrderReceived();
//        orderReceived.setOrderUri(MISSING_IMAGE_DELIVERY_ITEM_ID);
//
//        // When order-received message is sent to kafka topic
//        final List<Message> messages = sendAndConsumeMessage(ORDER_REFERENCE, MISSING_IMAGE_DELIVERY_ITEM_ID, item);
//
//        // Then we have successfully consumed a message.
//        assertThat(messages.isEmpty(), is(false));
//        final byte[] consumedMessageSerialized = messages.get(0).getValue();
//        final String deserializedConsumedMessage = new String(consumedMessageSerialized);
//
//        // And it matches the serialized order-received object
//        final AvroSerializer<OrderReceived> serializer =
//                serializerFactory.getGenericRecordSerializer(OrderReceived.class);
//        final byte[] orderReceivedSerialized = serializer.toBinary(orderReceived);
//        final String deserializedOrderReceived = new String(orderReceivedSerialized);
//
//        assertEquals(deserializedConsumedMessage, deserializedOrderReceived);
//    }
//
//    private List<Message> sendAndConsumeMessage(final String orderReference,
//                                                final String itemId,
//                                                final Item item) {
//        List<Message> messages;
//        testItemMessageConsumer.connect();
//        int count = 0;
//        do {
//            messages = testItemMessageConsumer.pollConsumerGroup();
//            itemMessageProducerUnderTest.sendMessage(orderReference, itemId, item);
//            count++;
//        } while (messages.isEmpty() && count < 15);
//
//        return messages;
//    }
//}
