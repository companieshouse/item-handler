package uk.gov.companieshouse.itemhandler.email;

public class MissingImageDeliveryOrderConfirmation extends OrderConfirmation {
    private MissingImage missingImage;

    public MissingImage getMissingImage() {
        return missingImage;
    }

    public void setMissingImage(MissingImage missingImage) {
        this.missingImage = missingImage;
    }
}
