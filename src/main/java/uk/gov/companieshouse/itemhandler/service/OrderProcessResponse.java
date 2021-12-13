package uk.gov.companieshouse.itemhandler.service;

public class OrderProcessResponse {
    private String orderUri;
    private Status status;

    private OrderProcessResponse(Builder builder) {
        orderUri = builder.orderUri;
        status = builder.status;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public enum Status { OK, SERVICE_UNAVAILABLE, SERVICE_ERROR};

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

    public String getOrderUri() {
        return orderUri;
    }

    public Status getStatus() {
        return status;
    }
}
