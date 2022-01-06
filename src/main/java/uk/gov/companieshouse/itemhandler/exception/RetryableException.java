package uk.gov.companieshouse.itemhandler.exception;

/**
 * Thrown to indicate a recoverable error in processing that can be tried again. An example of a recoverable error is a
 * network connectivity error while accessing an external api that may go away during subsequent retries.
 */
public class RetryableException extends ItemHandlerRuntimeException {

    public RetryableException(String message) {
        super(message);
    }

    public RetryableException(String message, Throwable cause) {
        super(message, cause);
    }

    public RetryableException(Throwable cause) {
        super(cause);
    }
}
