package uk.gov.companieshouse.itemhandler.model;

public class CertificateItemOptions extends DeliveryItemOptions {

    private CertificateType certificateType;
    private DirectorOrSecretaryDetails directorDetails;
    private Boolean includeCompanyObjectsInformation;
    private Boolean includeEmailCopy;
    private Boolean includeGoodStandingInformation;
    private RegisteredOfficeAddressDetails registeredOfficeAddressDetails;
    private DirectorOrSecretaryDetails secretaryDetails;
    public CertificateType getCertificateType() {
        return certificateType;
    }

    public void setCertificateType(CertificateType certificateType) {
        this.certificateType = certificateType;
    }


    public DirectorOrSecretaryDetails getDirectorDetails() {
        return directorDetails;
    }

    public void setDirectorDetails(DirectorOrSecretaryDetails directorOrSecretaryDetails) {
        this.directorDetails = directorOrSecretaryDetails;
    }

    public Boolean getIncludeCompanyObjectsInformation() {
        return includeCompanyObjectsInformation;
    }

    public void setIncludeCompanyObjectsInformation(Boolean includeCompanyObjectsInformation) {
        this.includeCompanyObjectsInformation = includeCompanyObjectsInformation;
    }

    public Boolean getIncludeEmailCopy() {
        return includeEmailCopy;
    }

    public void setIncludeEmailCopy(Boolean includeEmailCopy) {
        this.includeEmailCopy = includeEmailCopy;
    }

    public Boolean getIncludeGoodStandingInformation() {
        return includeGoodStandingInformation;
    }

    public void setIncludeGoodStandingInformation(Boolean includeGoodStandingInformation) {
        this.includeGoodStandingInformation = includeGoodStandingInformation;
    }

    public RegisteredOfficeAddressDetails getRegisteredOfficeAddressDetails() {
        return registeredOfficeAddressDetails;
    }

    public void setRegisteredOfficeAddressDetails(RegisteredOfficeAddressDetails registeredOfficeAddressDetails) {
        this.registeredOfficeAddressDetails = registeredOfficeAddressDetails;
    }

    public DirectorOrSecretaryDetails getSecretaryDetails() {
        return secretaryDetails;
    }

    public void setSecretaryDetails(DirectorOrSecretaryDetails secretaryDetails) {
        this.secretaryDetails = secretaryDetails;
    }

}
