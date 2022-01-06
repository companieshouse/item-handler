package uk.gov.companieshouse.itemhandler.exception;

public class SendKafkaMessageException extends RetryableException {

    public SendKafkaMessageException(String message) {
        super(message);
    }
}
