package uk.gov.companieshouse.itemhandler.model;

import com.fasterxml.jackson.annotation.JsonValue;

import static uk.gov.companieshouse.itemhandler.converter.EnumValueNameConverter.convertEnumValueNameToJson;

public enum CertificateType {
    INCORPORATION,
    INCORPORATION_WITH_ALL_NAME_CHANGES,
    INCORPORATION_WITH_LAST_NAME_CHANGES,
    DISSOLUTION_LIQUIDATION;

    @JsonValue
    public String getJsonName() {
        return convertEnumValueNameToJson(this);
    }
}
