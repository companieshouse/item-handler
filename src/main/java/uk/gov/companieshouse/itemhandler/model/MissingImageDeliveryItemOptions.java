package uk.gov.companieshouse.itemhandler.model;

import java.util.Map;

public class MissingImageDeliveryItemOptions extends ItemOptions{

    private String filingHistoryDate;

    private String filingHistoryDescription;

    private Map<String, Object> filingHistoryDescriptionValues;

    private String filingHistoryId;

    private String filingHistoryType;

    private String filingHistoryCategory;

    public String getFilingHistoryDate() {
        return filingHistoryDate;
    }

    public void setFilingHistoryDate(String filingHistoryDate) {
        this.filingHistoryDate = filingHistoryDate;
    }

    public String getFilingHistoryDescription() {
        return filingHistoryDescription;
    }

    public void setFilingHistoryDescription(String filingHistoryDescription) {
        this.filingHistoryDescription = filingHistoryDescription;
    }

    public Map<String, Object> getFilingHistoryDescriptionValues() {
        return filingHistoryDescriptionValues;
    }

    public void setFilingHistoryDescriptionValues(Map<String, Object> filingHistoryDescriptionValues) {
        this.filingHistoryDescriptionValues = filingHistoryDescriptionValues;
    }

    public String getFilingHistoryId() {
        return filingHistoryId;
    }

    public void setFilingHistoryId(String filingHistoryId) {
        this.filingHistoryId = filingHistoryId;
    }

    public String getFilingHistoryType() {
        return filingHistoryType;
    }

    public void setFilingHistoryType(String filingHistoryType) {
        this.filingHistoryType = filingHistoryType;
    }

    public String getFilingHistoryCategory() {
        return filingHistoryCategory;
    }

    public void setFilingHistoryCategory(String filingHistoryCategory) {
        this.filingHistoryCategory = filingHistoryCategory;
    }
}
