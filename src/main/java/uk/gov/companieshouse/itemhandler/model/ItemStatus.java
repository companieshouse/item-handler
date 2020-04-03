package uk.gov.companieshouse.itemhandler.model;

import com.fasterxml.jackson.annotation.JsonValue;

import static uk.gov.companieshouse.itemhandler.converter.EnumValueNameConverter.convertEnumValueNameToJson;

public enum ItemStatus {
    UNKNOWN,
    PROCESSING,
    SATISFIED;

    @JsonValue
    public String getJsonName() {
        return convertEnumValueNameToJson(this);
    }
}
