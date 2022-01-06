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
    private OrderReceived orderReceived;

    @Mock
    private ResponseHandlerConfig config;

    @Test
    void testServiceOkLogsDebugMessage() {
        //given
        when(message.getHeaders()).thenReturn(stubMessageHeaders());

        //when
        responseHandler.serviceOk(message);

        //then
        verify(logger).debug("Order received message processing completed", expectedLogData());
    }

    @Test
    void testServiceErrorLogsErrorMessage() {
        //given
        when(message.getHeaders()).thenReturn(stubMessageHeaders());

        //when
        responseHandler.serviceError(message);

        //then
        verify(logger).error("order-received message processing failed with a non-recoverable exception", expectedLogData());
    }

    @Test
    void testServiceUnavailableAttemptsRetryIfMaximumAttemptsNotExceeded() {
        //given
        when(message.getPayload()).thenReturn(orderReceived);
        when(message.getHeaders()).thenReturn(stubMessageHeaders());
        when(orderReceived.getAttempt()).thenReturn(1);
        when(config.getMaximumRetryAttempts()).thenReturn(3);
        when(config.getRetryTopic()).thenReturn("order-received-retry");

        //when
        responseHandler.serviceUnavailable(message);

        //then
        verify(orderReceived).setAttempt(2);
        verify(messageProducer).sendMessage(orderReceived, "order-received-retry");
        verify(logger).info("order-received message processing failed with a recoverable exception", expectedLogData());
    }

    @Test
    void testServiceUnavailableAttemptsRetryIfMaximumAttemptsExceeded() {
        //given
        when(message.getPayload()).thenReturn(orderReceived);
        when(message.getHeaders()).thenReturn(stubMessageHeaders());
        when(orderReceived.getAttempt()).thenReturn(3);
        when(config.getMaximumRetryAttempts()).thenReturn(3);
        when(config.getErrorTopic()).thenReturn("order-received-error");

        //when
        responseHandler.serviceUnavailable(message);

        //then
        verify(orderReceived).setAttempt(0);
        verify(messageProducer).sendMessage(orderReceived, "order-received-error");
        verify(logger).info("order-received message processing failed; maximum number of retry attempts exceeded", expectedLogData());
    }

    private MessageHeaders stubMessageHeaders() {
        Map<String, Object> headers = new HashMap<>();
        headers.put(KafkaHeaders.RECEIVED_MESSAGE_KEY, "key");
        headers.put(KafkaHeaders.RECEIVED_TOPIC, "topic");
        headers.put(KafkaHeaders.OFFSET, 1);
        headers.put(KafkaHeaders.RECEIVED_PARTITION_ID, 0);
        return new MessageHeaders(headers);
    }

    private Map<String, Object> expectedLogData() {
        Map<String, Object> logData = new HashMap<>();
        logData.put("key", "key");
        logData.put("topic", "topic");
        logData.put("offset", 1);
        logData.put("partition", 0);
        return logData;
    }
}
