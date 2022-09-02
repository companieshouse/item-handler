package uk.gov.companieshouse.itemhandler.itemsummary;

public class CertifiedCopyEmailConfig {
    private String recipient;
    private String standardSubjectLine;
    private String expressSubjectLine;
    private String dateFiledFormat;

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getStandardSubjectLine() {
        return standardSubjectLine;
    }

    public void setStandardSubjectLine(String standardSubjectLine) {
        this.standardSubjectLine = standardSubjectLine;
    }

    public String getExpressSubjectLine() {
        return expressSubjectLine;
    }

    public void setExpressSubjectLine(String expressSubjectLine) {
        this.expressSubjectLine = expressSubjectLine;
    }

    public String getDateFiledFormat() {
        return dateFiledFormat;
    }

    public void setDateFiledFormat(String dateFiledFormat) {
        this.dateFiledFormat = dateFiledFormat;
    }
}
