package uk.gov.companieshouse.itemhandler.exception;

public class SerialisationException extends NonRetryableException {

    public SerialisationException(String message) {
        super(message);
    }
}
