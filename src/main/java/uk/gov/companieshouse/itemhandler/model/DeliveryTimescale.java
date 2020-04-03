package uk.gov.companieshouse.itemhandler.model;

import com.fasterxml.jackson.annotation.JsonValue;

import static uk.gov.companieshouse.itemhandler.converter.EnumValueNameConverter.convertEnumValueNameToJson;

public enum DeliveryTimescale {
    STANDARD,
    SAME_DAY;

    @JsonValue
    public String getJsonName() {
        return convertEnumValueNameToJson(this);
    }
}
