package uk.gov.companieshouse.itemhandler.model;

public class CertificateSummary {
    private String itemNumber;
    private String certificateType;
    private String companyNumber;
    private String fee;

    public CertificateSummary() {
    }

    public CertificateSummary(String itemNumber, String certificateType, String companyNumber, String fee) {
        this.itemNumber = itemNumber;
        this.certificateType = certificateType;
        this.companyNumber = companyNumber;
        this.fee = fee;
    }

    public String getItemNumber() {
        return itemNumber;
    }

    public void setItemNumber(String itemNumber) {
        this.itemNumber = itemNumber;
    }

    public String getCertificateType() {
        return certificateType;
    }

    public void setCertificateType(String certificateType) {
        this.certificateType = certificateType;
    }

    public String getCompanyNumber() {
        return companyNumber;
    }

    public void setCompanyNumber(String companyNumber) {
        this.companyNumber = companyNumber;
    }

    public String getFee() {
        return fee;
    }

    public void setFee(String fee) {
        this.fee = fee;
    }
}
