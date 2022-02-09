package uk.gov.companieshouse.itemhandler.model;

public enum CompanyStatus {
    LIQUIDATION("liquidation"),
    OTHER("other");

    private final String value;

    CompanyStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
