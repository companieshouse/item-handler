package uk.gov.companieshouse.itemhandler.kafka;

import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.orders.OrderReceived;

public class OrderReceivedDeserialiser extends MessageDeserialiser<OrderReceived> {
    public OrderReceivedDeserialiser(Logger logger) {
        super(OrderReceived.class);
    }
}
