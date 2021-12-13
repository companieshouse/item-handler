package uk.gov.companieshouse.itemhandler.kafka;

import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.service.OrderProcessResponse;
import uk.gov.companieshouse.orders.OrderReceived;

@Service
class OrderProcessResponseHandler implements OrderProcessResponse.Visitor {
    @Override
    public void serviceOk(Message<OrderReceived> message) {

    }

    @Override
    public void serviceUnavailable(Message<OrderReceived> message) {

    }

    @Override
    public void serviceError(Message<OrderReceived> message) {

    }
}
