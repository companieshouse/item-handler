package uk.gov.companieshouse.itemhandler.model;

import com.fasterxml.jackson.annotation.JsonValue;

import static uk.gov.companieshouse.itemhandler.converter.EnumValueNameConverter.convertEnumValueNameToJson;

public enum CompanyType {
    LIMITED_LIABILITY_PARTNERSHIP,
    LIMITED_PARTNERSHIP,
    OTHER;

    @JsonValue
    public String getJsonName() {
        return convertEnumValueNameToJson(this);
    }
}
