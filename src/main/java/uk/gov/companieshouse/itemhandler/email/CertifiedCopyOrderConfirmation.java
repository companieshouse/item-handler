package uk.gov.companieshouse.itemhandler.email;

import java.util.List;

public class CertifiedCopyOrderConfirmation extends OrderConfirmation {
    private String companyName;
    private String companyNumber;
    private List<CertifiedDocument> certifiedDocuments;
    private String totalFee;

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
}
