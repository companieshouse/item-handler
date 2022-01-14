package uk.gov.companieshouse.itemhandler.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import uk.gov.companieshouse.itemhandler.config.ResponseHandlerConfig;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.orders.OrderReceived;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderProcessResponseHandlerTest {

    @InjectMocks
    private OrderProcessResponseHandler responseHandler;

    @Mock
    private OrderMessageProducer messageProducer;

    @Mock
    private Logger logger;

    @Mock
    private Message<OrderReceived> message;

    @Mock
    private ResponseHandlerConfig config;

    @Test
    void testServiceOkLogsDebugMessage() {
        //given
        OrderReceived orderReceived = new OrderReceived();
        orderReceived.setAttempt(0);
        when(message.getPayload()).thenReturn(orderReceived);
        when(message.getHeaders()).thenReturn(stubMessageHeaders());

        //when
        responseHandler.serviceOk(message);

        //then
        assertEquals(0, orderReceived.getAttempt());
        verify(logger).debug("Order received message processing completed", expectedLogData(0));
    }

    @Test
    void testServiceErrorLogsErrorMessage() {
        //given
        OrderReceived orderReceived = new OrderReceived();
        orderReceived.setAttempt(0);
        when(message.getPayload()).thenReturn(orderReceived);
        when(message.getHeaders()).thenReturn(stubMessageHeaders());

        //when
        responseHandler.serviceError(message);

        //then
        assertEquals(0, orderReceived.getAttempt());
        verify(logger).error("order-received message processing failed with a non-recoverable exception", expectedLogData(0));
    }

    @Test
    void testServiceUnavailableAttemptsRetryIfMaximumAttemptsNotExceeded() {
        //given
        OrderReceived orderReceived = new OrderReceived();
        orderReceived.setAttempt(1);
        when(message.getPayload()).thenReturn(orderReceived);
        when(message.getHeaders()).thenReturn(stubMessageHeaders());
        when(config.getMaximumRetryAttempts()).thenReturn(3);
        when(config.getRetryTopic()).thenReturn("order-received-retry");

        //when
        responseHandler.serviceUnavailable(message);

        //then
        assertEquals(2, orderReceived.getAttempt());
        verify(messageProducer).sendMessage(orderReceived, "order-received-retry");
        verify(logger).info("publish order received to retry topic", expectedLogData(2));
    }

    @Test
    void testServiceUnavailablePublishesToErrorTopicIfMaximumAttemptsExceeded() {
        //given
        OrderReceived orderReceived = new OrderReceived();
        orderReceived.setAttempt(3);
        when(message.getPayload()).thenReturn(orderReceived);
        when(message.getHeaders()).thenReturn(stubMessageHeaders());
        when(config.getMaximumRetryAttempts()).thenReturn(3);
        when(config.getErrorTopic()).thenReturn("order-received-error");

        //when
        responseHandler.serviceUnavailable(message);

        //then
        assertEquals(0, orderReceived.getAttempt());
        verify(messageProducer).sendMessage(orderReceived, "order-received-error");
        verify(logger).info("publish order received to error topic", expectedLogData(0));
    }

    private MessageHeaders stubMessageHeaders() {
        Map<String, Object> headers = new HashMap<>();
        headers.put(KafkaHeaders.RECEIVED_MESSAGE_KEY, "key");
        headers.put(KafkaHeaders.RECEIVED_TOPIC, "topic");
        headers.put(KafkaHeaders.OFFSET, 1);
        headers.put(KafkaHeaders.RECEIVED_PARTITION_ID, 0);
        return new MessageHeaders(headers);
    }

    private Map<String, Object> expectedLogData(int retryAttempt) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("key", "key");
        logData.put("topic", "topic");
        logData.put("offset", 1);
        logData.put("partition", 0);
        logData.put("retry_attempt", retryAttempt);
        return logData;
    }
}