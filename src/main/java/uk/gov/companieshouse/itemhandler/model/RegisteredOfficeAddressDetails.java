package uk.gov.companieshouse.itemhandler.model;

import com.google.gson.Gson;

public class RegisteredOfficeAddressDetails implements Address {
    private IncludeAddressRecordsType includeAddressRecordsType;

    private Boolean includeDates;

    @Override
    public IncludeAddressRecordsType getIncludeAddressRecordsType() {
        return includeAddressRecordsType;
    }

    public void setIncludeAddressRecordsType(IncludeAddressRecordsType includeAddressRecordsType) {
        this.includeAddressRecordsType = includeAddressRecordsType;
    }

    @Override
    public Boolean getIncludeDates() {
        return includeDates;
    }

    public void setIncludeDates(Boolean includeDates) {
        this.includeDates = includeDates;
    }

    @Override
    public String toString() { return new Gson().toJson(this); }

}
