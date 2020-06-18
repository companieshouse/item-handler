package uk.gov.companieshouse.itemhandler.exception;

/**
 * Represents any error/exception encountered when producing messages to the Kafka `email-send` topic that could be
 * transient. Given that such errors may be temporary in nature, this exception type is used to indicate that the
 * message could be retried.
 */
public class RetryableEmailSendException extends RetryableErrorException {
    public RetryableEmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
