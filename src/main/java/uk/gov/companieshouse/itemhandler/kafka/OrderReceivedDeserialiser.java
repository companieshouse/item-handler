package uk.gov.companieshouse.itemhandler.kafka;

import uk.gov.companieshouse.orders.OrderReceived;

public class OrderReceivedDeserialiser extends MessageDeserialiser<OrderReceived> {
    public OrderReceivedDeserialiser() {
        super(OrderReceived.class);
    }
}
