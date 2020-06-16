package uk.gov.companieshouse.itemhandler.exception;

/**
 * Represents any error/exception encountered when sending requests to the Orders API that could be transient.
 * Given that such errors may be temporary in nature, this exception type is used to indicate that the request
 * could be retried.
 */
public class RetryableOrdersApiException extends RetryableErrorException {
    public RetryableOrdersApiException(String message) {
        super(message);
    }
}
