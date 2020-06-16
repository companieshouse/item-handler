package uk.gov.companieshouse.itemhandler.kafka;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import uk.gov.companieshouse.kafka.consumer.resilience.CHConsumerType;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka
@TestPropertySource(properties={"uk.gov.companieshouse.item-handler.error-consumer=true",
                                "certificate.order.confirmation.recipient = nobody@nowhere.com"})
public class OrdersKafkaConsumerIntegrationErrorModeTest {
    private static final String ORDER_RECEIVED_TOPIC = "order-received";
    private static final String ORDER_RECEIVED_TOPIC_RETRY = "order-received-retry";
    private static final String ORDER_RECEIVED_TOPIC_ERROR = "order-received-error";
    private static final String ORDER_RECEIVED_URI = "/orders/ORDER-12345";
    private static final String ORDER_RECEIVED_MESSAGE_JSON = "{\"order_uri\": \"/orders/ORDER-12345\"}";

    @Autowired
    private OrdersKafkaProducer kafkaProducer;

    @Autowired
    private OrdersKafkaConsumerWrapper consumerWrapper;

    @AfterEach
    public void tearDown() {
        consumerWrapper.reset();
    }

    @Test
    @DisplayName("order-received topic consumer does not receive message when 'error-consumer' (env var IS_ERROR_QUEUE_CONSUMER)is true")
    public void testOrdersConsumerReceivesOrderReceivedMessage1() throws InterruptedException, ExecutionException, SerializationException {
        // When
        kafkaProducer.sendMessage(consumerWrapper.createMessage(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC));

        // Then
        verifyProcessOrderReceivedNotInvoked(CHConsumerType.MAIN_CONSUMER);
    }

    @Test
    @DisplayName("order-received-retry topic consumer does not receive message when 'error-consumer' (env var IS_ERROR_QUEUE_CONSUMER)is true")
    public void testOrdersConsumerReceivesOrderReceivedMessage2Retry() throws InterruptedException, SerializationException, ExecutionException {
        // When
        kafkaProducer.sendMessage(consumerWrapper.createMessage(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC_RETRY));

        // Then
        verifyProcessOrderReceivedNotInvoked(CHConsumerType.RETRY_CONSUMER);
    }

    private void verifyProcessOrderReceivedNotInvoked(CHConsumerType type) throws InterruptedException {
        consumerWrapper.setTestType(type);
        consumerWrapper.getLatch().await(3000, TimeUnit.MILLISECONDS);
        assertThat(consumerWrapper.getLatch().getCount(), is(equalTo(1L)));
        final String processedOrderUri = consumerWrapper.getOrderUri();
        assertThat(processedOrderUri, isEmptyOrNullString());
    }

    @Test
    @DisplayName("order-received-error topic consumer receives message when 'error-consumer' (env var IS_ERROR_QUEUE_CONSUMER) is true")
    public void testOrdersConsumerReceivesOrderReceivedMessage3Error() throws InterruptedException, ExecutionException, SerializationException {
        // When
        kafkaProducer.sendMessage(consumerWrapper.createMessage(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC_ERROR));

        // Then
        verifyProcessOrderReceivedInvoked(CHConsumerType.ERROR_CONSUMER);
    }

    private void verifyProcessOrderReceivedInvoked(CHConsumerType type) throws InterruptedException {
        consumerWrapper.setTestType(type);
        consumerWrapper.getLatch().await(3000, TimeUnit.MILLISECONDS);
        assertThat(consumerWrapper.getLatch().getCount(), is(equalTo(0L)));
        final String processedOrderUri = consumerWrapper.getOrderUri();
        assertThat(processedOrderUri, is(equalTo(ORDER_RECEIVED_MESSAGE_JSON)));
    }
}
