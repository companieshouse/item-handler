package uk.gov.companieshouse.itemhandler.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.config.ResponseHandlerConfig;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.itemhandler.service.OrderProcessResponse;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.orders.OrderReceived;

@Service
class OrderProcessResponseHandler implements OrderProcessResponse.Visitor {

    private final OrderMessageProducer messageProducer;
    private final ResponseHandlerConfig config;
    private final Logger logger;

    @Autowired
    public OrderProcessResponseHandler(OrderMessageProducer messageProducer, ResponseHandlerConfig config, Logger logger) {
        this.messageProducer = messageProducer;
        this.config = config;
        this.logger = logger;
    }

    @Override
    public void serviceOk(Message<OrderReceived> message) {
        logger.debug("Order received message processing completed", LoggingUtils.getMessageHeadersAsMap(message));
    }

    @Override
    public void serviceUnavailable(Message<OrderReceived> message) {
        OrderReceived payload = message.getPayload();
        if(payload.getAttempt() < config.getMaximumRetryAttempts()) {
            publishToRetryTopic(message, payload);
        } else {
            publishToErrorTopic(message, payload);
        }
    }

    @Override
    public void serviceError(Message<OrderReceived> message) {
        logger.error("order-received message processing failed with a non-recoverable exception", LoggingUtils.getMessageHeadersAsMap(message));
    }

    protected void publishToRetryTopic(Message<OrderReceived> message, OrderReceived payload) {
        logger.info("order-received message processing failed with a recoverable exception", LoggingUtils.getMessageHeadersAsMap(message));
        payload.setAttempt(payload.getAttempt() + 1);
        messageProducer.sendMessage(payload, config.getRetryTopic());
    }

    protected void publishToErrorTopic(Message<OrderReceived> message, OrderReceived payload) {
        logger.info("order-received message processing failed; maximum number of retry attempts exceeded", LoggingUtils.getMessageHeadersAsMap(message));
        payload.setAttempt(0);
        messageProducer.sendMessage(payload, config.getErrorTopic());
    }
}
