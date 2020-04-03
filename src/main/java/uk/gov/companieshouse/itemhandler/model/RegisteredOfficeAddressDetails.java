package uk.gov.companieshouse.itemhandler.model;

import com.google.gson.Gson;

public class RegisteredOfficeAddressDetails {
    private IncludeAddressRecordsType includeAddressRecordsType;

    private Boolean includeDates;

    public IncludeAddressRecordsType getIncludeAddressRecordsType() {
        return includeAddressRecordsType;
    }

    public void setIncludeAddressRecordsType(IncludeAddressRecordsType includeAddressRecordsType) {
        this.includeAddressRecordsType = includeAddressRecordsType;
    }

    public Boolean getIncludeDates() {
        return includeDates;
    }

    public void setIncludeDates(Boolean includeDates) {
        this.includeDates = includeDates;
    }

    @Override
    public String toString() { return new Gson().toJson(this); }

}
