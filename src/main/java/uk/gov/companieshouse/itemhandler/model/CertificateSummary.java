package uk.gov.companieshouse.itemhandler.model;

import java.util.Objects;

public class CertificateSummary {
    private String itemNumber;
    private CertificateType certificateType;
    private String companyNumber;
    private String fee;

    public CertificateSummary() {
    }

    public CertificateSummary(String itemNumber, CertificateType certificateType, String companyNumber, String fee) {
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

    public CertificateType getCertificateType() {
        return certificateType;
    }

    public void setCertificateType(CertificateType certificateType) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CertificateSummary that = (CertificateSummary) o;
        return Objects.equals(itemNumber, that.itemNumber)
                && Objects.equals(certificateType, that.certificateType)
                && Objects.equals(companyNumber, that.companyNumber)
                && Objects.equals(fee, that.fee);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemNumber, certificateType, companyNumber, fee);
    }

    @Override
    public String toString() {
        return "CertificateSummary{" +
                "itemNumber='" + itemNumber + '\'' +
                ", certificateType=" + certificateType +
                ", companyNumber='" + companyNumber + '\'' +
                ", fee='" + fee + '\'' +
                '}';
    }
}
