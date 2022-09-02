package uk.gov.companieshouse.itemhandler.itemsummary;

import java.util.Objects;

public class CertificateSummary {
    private String itemNumber;
    private String certificateType;
    private String companyNumber;
    private String fee;
    private String viewCertificateLink;

    public CertificateSummary() {
    }

    public CertificateSummary(String itemNumber, String certificateType, String companyNumber, String fee, String viewCertificateLink) {
        this.itemNumber = itemNumber;
        this.certificateType = certificateType;
        this.companyNumber = companyNumber;
        this.fee = fee;
        this.viewCertificateLink = viewCertificateLink;
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

    public String getViewCertificateLink() {
        return viewCertificateLink;
    }

    public void setViewCertificateLink(String viewCertificateLink) {
        this.viewCertificateLink = viewCertificateLink;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CertificateSummary)) {
            return false;
        }
        CertificateSummary that = (CertificateSummary) o;
        return Objects.equals(getItemNumber(), that.getItemNumber())
                && Objects.equals(getCertificateType(), that.getCertificateType())
                && Objects.equals(getCompanyNumber(), that.getCompanyNumber())
                && Objects.equals(getFee(), that.getFee())
                && Objects.equals(getViewCertificateLink(), that.getViewCertificateLink());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getItemNumber(), getCertificateType(), getCompanyNumber(), getFee(), getViewCertificateLink());
    }
}
