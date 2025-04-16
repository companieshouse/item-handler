package uk.gov.companieshouse.itemhandler.exception;

public class EmailClientException extends RuntimeException {

    public EmailClientException(String message, Throwable cause) {
        super(message, cause);
    }

}
