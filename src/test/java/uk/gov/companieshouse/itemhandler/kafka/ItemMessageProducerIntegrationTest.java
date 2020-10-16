package uk.gov.companieshouse.itemhandler.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import uk.gov.companieshouse.itemhandler.model.ActionedBy;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.ItemCosts;
import uk.gov.companieshouse.itemhandler.model.ItemLinks;
import uk.gov.companieshouse.itemhandler.model.MissingImageDeliveryItemOptions;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.serialization.AvroSerializer;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.orders.items.ChdItemOrdered;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;
import static uk.gov.companieshouse.itemhandler.model.ProductType.MISSING_IMAGE_DELIVERY_ACCOUNTS;
import static uk.gov.companieshouse.itemhandler.util.TestConstants.MISSING_IMAGE_DELIVERY_ITEM_ID;
import static uk.gov.companieshouse.itemhandler.util.TestConstants.ORDER_REFERENCE;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka
class ItemMessageProducerIntegrationTest {

    @Configuration
    @ComponentScan(basePackageClasses = ItemMessageProducerIntegrationTest.class,
                   excludeFilters = {@ComponentScan.Filter(type = ASSIGNABLE_TYPE,
                                                           value = OrdersKafkaConsumerWrapper.class),
                                     @ComponentScan.Filter(type = ASSIGNABLE_TYPE,
                                                           value = OrdersKafkaConsumer.class)})
    public static class Config {
        @Bean
        public SerializerFactory getSerializerFactory() {
            return new SerializerFactory();
        }

        @Bean
        public ItemMessageProducer itemMessageProducerUnderTest() {
            return new ItemMessageProducer(getItemMessageFactory(), getItemKafkaProducer());
        }

        @Bean
        public ItemMessageFactory getItemMessageFactory() {
            return new ItemMessageFactory(getSerializerFactory(), getObjectMapper());
        }

        @Bean
        public ItemKafkaProducer getItemKafkaProducer() {
            return new ItemKafkaProducer();
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
    void testSendItemMessageToKafkaTopic() throws Exception {

        // Given an item from the order is to be sent
        final OrderData order = new OrderData();
        final Item item = new Item();
        item.setId(MISSING_IMAGE_DELIVERY_ITEM_ID);
        order.setItems(singletonList(item));
        order.setOrderedAt(LocalDateTime.now());
        final ActionedBy orderedBy = new ActionedBy();
        orderedBy.setEmail("demo@ch.gov.uk");
        orderedBy.setId("4Y2VkZWVlMzhlZWFjY2M4MzQ3M1234");
        order.setOrderedBy(orderedBy);
        final ItemCosts costs = new ItemCosts("0", "3", "3", MISSING_IMAGE_DELIVERY_ACCOUNTS);
        item.setItemCosts(singletonList(costs));
        final MissingImageDeliveryItemOptions options = new MissingImageDeliveryItemOptions();
        item.setItemOptions(options);
        final ItemLinks links = new ItemLinks();
        links.setSelf("/orderable/missing-image-deliveries/MID-535516-028321");
        item.setLinks(links);
        item.setQuantity(1);
        item.setCompanyName("THE GIRLS' DAY SCHOOL TRUST");
        item.setCompanyNumber("00006400");
        item.setCustomerReference("MID ordered by VJ GCI-1301");
        item.setDescription("missing image delivery for company 00006400");
        item.setDescriptionIdentifier("missing-image-delivery");
        final Map<String, String> descriptionValues = new HashMap<>();
        descriptionValues.put("company_number", "00006400");
        descriptionValues.put("missing-image-delivery", "missing image delivery for company 00006400");
        item.setDescriptionValues(descriptionValues);
        item.setItemUri("/orderable/missing-image-deliveries/MID-535516-028321");
        item.setKind("item#missing-image-delivery");
        item.setTotalItemCost("3");
        order.setPaymentReference("1234");
        order.setReference("ORD-968316-028323");
        order.setTotalOrderCost("3");

        // Given that the actual message produced to the topic is a ChdItemOrdered object.
        final ChdItemOrdered chdItemOrdered = itemMessageFactory.createChdItemOrdered(order);

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
