package uk.gov.companieshouse.itemhandler.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.itemhandler.kafka.OrderReceivedMessageProducer;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.orders.OrderReceived;

import java.util.concurrent.ExecutionException;

import static uk.gov.companieshouse.itemhandler.ItemHandlerApplication.APPLICATION_NAMESPACE;

/**
 * TODO GCI-931 Remove this and its entry from routes.yaml
 */
@RestController
public class TestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);

    @Autowired
    OrderReceivedMessageProducer ordersMessageProducer;

    @GetMapping("/test")
    public ResponseEntity<Void> getHealthCheck () throws InterruptedException, ExecutionException, SerializationException {
        LOGGER.info("Test invoked.");
        final OrderReceived orderReceived = new OrderReceived();
        orderReceived.setOrderUri("ORDER_URI");
        ordersMessageProducer.sendMessage(orderReceived);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
