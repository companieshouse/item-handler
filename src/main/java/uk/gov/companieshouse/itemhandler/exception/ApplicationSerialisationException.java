package uk.gov.companieshouse.itemhandler.exception;

public class ApplicationSerialisationException extends NonRetryableException {

    public ApplicationSerialisationException(String message) {
        super(message);
    }
}
