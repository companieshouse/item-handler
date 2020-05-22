package uk.gov.companieshouse.itemhandler.exception;

public class RetryableErrorException extends RuntimeException {
    public RetryableErrorException(String message) {
        super(message);
    }
}
