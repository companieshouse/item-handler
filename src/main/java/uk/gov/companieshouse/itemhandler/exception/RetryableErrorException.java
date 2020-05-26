package uk.gov.companieshouse.itemhandler.exception;

public class RetryableErrorException extends RuntimeException {
    /**
     * Thrown to indicate some error in processing that can be be recovered from if tried again. For example, a network
     * connectivity error that may go away during subsequent retries.
     * @param message error message
     */
    public RetryableErrorException(String message) {
        super(message);
    }
}
