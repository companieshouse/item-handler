package uk.gov.companieshouse.itemhandler.kafka;

public class OrderProcessingException extends RuntimeException {
    public OrderProcessingException() { super("Order processing failed."); }
}
