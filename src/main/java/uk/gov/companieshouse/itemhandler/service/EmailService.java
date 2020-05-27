package uk.gov.companieshouse.itemhandler.service;

import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.exception.RetryableErrorException;
import uk.gov.companieshouse.itemhandler.kafka.OrdersKafkaRetryable;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.orders.OrderReceived;

import static uk.gov.companieshouse.itemhandler.ItemHandlerApplication.APPLICATION_NAMESPACE;

@Service
public class EmailService implements OrdersKafkaRetryable {
    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);

    @Override
    public void processMessage(Message<OrderReceived> message) throws RetryableErrorException {
        OrderReceived msg = message.getPayload();
        String orderReceivedUri = msg.getOrderUri();
        LOGGER.info(String.format("Message \"%1$s\" received for processing.", orderReceivedUri));
        throw new RetryableErrorException("Mock exception");
    }
}
