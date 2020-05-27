package uk.gov.companieshouse.itemhandler.kafka;

import org.springframework.messaging.Message;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import uk.gov.companieshouse.itemhandler.exception.RetryableErrorException;
import uk.gov.companieshouse.orders.OrderReceived;

public interface OrdersKafkaRetryable {
    /**
     * A retryable method that runs itself `maxAttempts` times when its implementation throws a RetryableErrorException.
     * By default `maxAttempts`, a `@Retryable` property, is set to 3.
     * @param message message to be processed
     * @throws Exception
     */
    @Retryable(value = RetryableErrorException.class, backoff = @Backoff(delay = 5000))
    void processMessage(Message<OrderReceived> message) throws Exception;
}
