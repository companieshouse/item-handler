package uk.gov.companieshouse.itemhandler.exception;

public class ItemHandlerRuntimeException extends RuntimeException {

    public ItemHandlerRuntimeException(String message) {
        super(message);
    }

    public ItemHandlerRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ItemHandlerRuntimeException(Throwable cause) {
        super(cause);
    }
}
