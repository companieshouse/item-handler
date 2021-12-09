package uk.gov.companieshouse.itemhandler.exception;

public class ApiException extends RetryableException {

    public ApiException(String message) {
        super(message);
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApiException(Throwable cause) {
        super(cause);
    }
}
