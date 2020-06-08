package uk.gov.companieshouse.itemhandler.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.serialization.AvroSerializer;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.orders.OrderReceived;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka
@TestPropertySource(properties="certificate.order.confirmation.recipient = nobody@nowhere.com")
public class OrdersMessageProducerIntegrationTest {
    private static final String ORDER_URI = "/order/ORDER-12345";
    private static final String ORDER_TOPIC = "order-received";

    @Autowired
    OrdersKafkaProducer kafkaProducerUnderTest;

    @Autowired
    OrdersKafkaConsumer kafkaConsumer;

    @Autowired
    TestOrdersMessageConsumer testOrdersMessageConsumer;

    @Autowired
    SerializerFactory serializerFactory;

    @Test
    void testSendOrderReceivedMessageToKafkaTopic() throws Exception {
        // Given an order is generated
        OrderReceived orderReceived = new OrderReceived();
        orderReceived.setOrderUri(ORDER_URI);

        // When order-received message is sent to kafka topic
        List<Message> messages = sendAndConsumeMessage();

        // Then we have successfully consumed a message.
        assertThat(messages.isEmpty(), is(false));
        byte[] consumedMessageSerialized = messages.get(0).getValue();
        String deserializedConsumedMessage = new String(consumedMessageSerialized);

        // And it matches the serialized order-received object
        AvroSerializer<OrderReceived> serializer = serializerFactory.getGenericRecordSerializer(OrderReceived.class);
        byte[] orderReceivedSerialized = serializer.toBinary(orderReceived);
        String deserializedOrderReceived = new String(orderReceivedSerialized);

        assertEquals(deserializedConsumedMessage, deserializedOrderReceived);
    }

    private List<Message> sendAndConsumeMessage() throws Exception {
        List<Message> messages;
        testOrdersMessageConsumer.connect();
        int count = 0;
        do {
            messages = testOrdersMessageConsumer.pollConsumerGroup();
            kafkaProducerUnderTest.sendMessage(kafkaConsumer.createRetryMessage(ORDER_URI, ORDER_TOPIC));
            count++;
        } while(messages.isEmpty() && count < 15);

        return messages;
    }
}
