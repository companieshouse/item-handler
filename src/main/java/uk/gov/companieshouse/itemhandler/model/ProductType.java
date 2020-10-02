package uk.gov.companieshouse.itemhandler.model;

import com.fasterxml.jackson.annotation.JsonValue;

import static uk.gov.companieshouse.itemhandler.converter.EnumValueNameConverter.convertEnumValueNameToJson;

/**
 * Values of this represent the possible product types.
 */
public enum ProductType {
    CERTIFICATE,
    CERTIFICATE_SAME_DAY,
    CERTIFICATE_ADDITIONAL_COPY,
    MISSING_IMAGE_DELIVERY,
    CERTIFIED_COPY,
    CERTIFIED_COPY_SAME_DAY,
    CERTIFIED_COPY_INCORPORATION,
    CERTIFIED_COPY_INCORPORATION_SAME_DAY;

    @JsonValue
    public String getJsonName() {
        return convertEnumValueNameToJson(this);
    }
}
