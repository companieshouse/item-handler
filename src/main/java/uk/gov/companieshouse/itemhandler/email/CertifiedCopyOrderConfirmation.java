package uk.gov.companieshouse.itemhandler.email;

import java.util.List;

public class CertifiedCopyOrderConfirmation extends OrderConfirmation {
    private List<CertifiedDocument> certifiedDocuments;
    private String totalFee;
    private String deliveryMethod;

    public List<CertifiedDocument> getCertifiedDocuments() {
        return certifiedDocuments;
    }

    public void setCertifiedDocuments(List<CertifiedDocument> certifiedDocuments) {
        this.certifiedDocuments = certifiedDocuments;
    }

    public String getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(String totalFee) {
        this.totalFee = totalFee;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }
}
