package uk.gov.companieshouse.itemhandler.exception;

/**
 * Represents any error/exception encountered when sending requests to the Orders API that we do not think will be
 * transient.
 */
public class OrdersApiException extends RuntimeException {
    public OrdersApiException(String message) {
        super(message);
    }
}
