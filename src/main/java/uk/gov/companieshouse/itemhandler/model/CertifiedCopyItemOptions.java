package uk.gov.companieshouse.itemhandler.model;

import java.util.List;

public class CertifiedCopyItemOptions extends ItemOptions {

    private List<FilingHistoryDocument> filingHistoryDocuments;

    public List<FilingHistoryDocument> getFilingHistoryDocuments() {
        return filingHistoryDocuments;
    }

    public void setFilingHistoryDocuments(List<FilingHistoryDocument> filingHistoryDocuments) {
        this.filingHistoryDocuments = filingHistoryDocuments;
    }
}
