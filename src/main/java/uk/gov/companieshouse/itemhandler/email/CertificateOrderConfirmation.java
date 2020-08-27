package uk.gov.companieshouse.itemhandler.email;

/**
 * An instance of this holds the information required to generate a certificate order confirmation email.
 */
public class CertificateOrderConfirmation extends OrderConfirmation {

    private String feeAmount;
    private String certificateType;
    private String[] certificateIncludes;

    public String getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(String feeAmount) {
        this.feeAmount = feeAmount;
    }

    public String getCertificateType() {
        return certificateType;
    }

    public void setCertificateType(String certificateType) {
        this.certificateType = certificateType;
    }

    public String[] getCertificateIncludes() {
        return certificateIncludes;
    }

    public void setCertificateIncludes(String[] certificateIncludes) {
        this.certificateIncludes = certificateIncludes;
    }
}
