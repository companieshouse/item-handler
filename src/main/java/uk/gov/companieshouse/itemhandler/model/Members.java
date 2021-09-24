package uk.gov.companieshouse.itemhandler.model;

public interface Members extends BasicInformationIncludable {
    Boolean getIncludeAddress();

    Boolean getIncludeAppointmentDate();

    Boolean getIncludeCountryOfResidence();

    IncludeDobType getIncludeDobType();
}
