package uk.gov.companieshouse.itemhandler.model;

public enum ItemType {
    CERTIFICATE("item#certificate"),
    CERTIFIED_COPY("item#certified-copy");

    ItemType(final String kind) {
        this.kind = kind;
    }

    private String kind;

    public String getKind(){
        return this.kind;
    }
}
