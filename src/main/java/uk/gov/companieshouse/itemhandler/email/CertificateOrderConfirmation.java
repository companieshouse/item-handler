package uk.gov.companieshouse.itemhandler.email;

/**
 * An instance of this holds the information required to generate a certificate order confirmation email.
 */
public class CertificateOrderConfirmation extends OrderConfirmation {

    private String feeAmount;
    private String companyName;
    private String companyNumber;
    private String certificateType;
    private String[] certificateIncludes;

    public String getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(String feeAmount) {
        this.feeAmount = feeAmount;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyNumber() {
        return companyNumber;
    }

    public void setCompanyNumber(String companyNumber) {
        this.companyNumber = companyNumber;
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
