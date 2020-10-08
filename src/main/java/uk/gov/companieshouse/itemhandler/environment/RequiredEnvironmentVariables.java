package uk.gov.companieshouse.itemhandler.environment;

public enum RequiredEnvironmentVariables {
    
    IS_ERROR_QUEUE_CONSUMER("IS_ERROR_QUEUE_CONSUMER"),
    CHS_API_KEY("CHS_API_KEY"),
    CERTIFICATE_ORDER_CONFIRMATION_RECIPIENT("CERTIFICATE_ORDER_CONFIRMATION_RECIPIENT"),
    CERTIFIED_COPY_ORDER_CONFIRMATION_RECIPIENT("CERTIFIED_COPY_ORDER_CONFIRMATION_RECIPIENT"),
    MISSING_IMAGE_DELIVERY_ORDER_CONFIRMATION_RECIPIENT("MISSING_IMAGE_DELIVERY_ORDER_CONFIRMATION_RECIPIENT");

    private String name;
    
    RequiredEnvironmentVariables(String name){
        this.name = name;
    }
    
    public String getName() {
        return name;
    }

}
