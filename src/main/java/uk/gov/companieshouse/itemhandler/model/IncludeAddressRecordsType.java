package uk.gov.companieshouse.itemhandler.model;

import com.fasterxml.jackson.annotation.JsonValue;

import static uk.gov.companieshouse.itemhandler.converter.EnumValueNameConverter.convertEnumValueNameToJson;

public enum IncludeAddressRecordsType {
    CURRENT,
    CURRENT_AND_PREVIOUS,
    CURRENT_PREVIOUS_AND_PRIOR,
    ALL;

    @JsonValue
    public String getJsonName() {
        return convertEnumValueNameToJson(this);
    }
}
