package uk.gov.companieshouse.itemhandler.service;

import org.springframework.messaging.Message;
import uk.gov.companieshouse.orders.OrderReceived;

public class OrderProcessResponse {
    private final String orderUri;
    private final Status status;

    private OrderProcessResponse(Builder builder) {
        orderUri = builder.orderUri;
        status = builder.status;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getOrderUri() {
        return orderUri;
    }

    public Status getStatus() {
        return status;
    }

    public enum Status implements Accept {
        OK {
            @Override
            public void accept(Visitor visitor, Message<OrderReceived> message) {
                visitor.serviceOk(message);
            }
        }, SERVICE_UNAVAILABLE {
            @Override
            public void accept(Visitor visitor, Message<OrderReceived> message) {
                visitor.serviceUnavailable(message);
            }
        }, SERVICE_ERROR {
            @Override
            public void accept(Visitor visitor, Message<OrderReceived> message) {
                visitor.serviceError(message);
            }
        }
    }

    public interface Accept {
        void accept(Visitor visitor, Message<OrderReceived> message);
    }

    public interface Visitor {
        void serviceOk(Message<OrderReceived> message);

        void serviceUnavailable(Message<OrderReceived> message);

        void serviceError(Message<OrderReceived> message);
    }

    public static final class Builder {
        private String orderUri;
        private Status status;

        private Builder() {
        }

        public Builder withOrderUri(String orderUri) {
            this.orderUri = orderUri;
            return this;
        }

        public Builder withStatus(Status status) {
            this.status = status;
            return this;
        }

        public OrderProcessResponse build() {
            return new OrderProcessResponse(this);
        }
    }
}
