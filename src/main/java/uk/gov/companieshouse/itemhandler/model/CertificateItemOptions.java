package uk.gov.companieshouse.itemhandler.model;

public class CertificateItemOptions extends DeliveryItemOptions {
    private CertificateType certificateType;
    private DirectorOrSecretaryDetails directorDetails;
    private Boolean includeCompanyObjectsInformation;
    private Boolean includeEmailCopy;
    private Boolean includeGoodStandingInformation;
    private RegisteredOfficeAddressDetails registeredOfficeAddressDetails;
    private DirectorOrSecretaryDetails secretaryDetails;
    private CompanyType companyType;
    private DesignatedMembersDetails designatedMembersDetails;
    private MembersDetails membersDetails;
    private GeneralPartnerDetails generalPartnerDetails;
    private LimitedPartnerDetails limitedPartnerDetails;
    private PrincipalPlaceOfBusinessDetails principalPlaceOfBusinessDetails;
    private Boolean includeGeneralNatureOfBusinessInformation;
    private LiquidatorsDetails liquidatorsDetails;
    private AdministratorsDetails administratorsDetails;
    private CompanyStatus companyStatus;

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

    public CompanyType getCompanyType() {
        return companyType;
    }

    public void setCompanyType(CompanyType companyType) {
        this.companyType = companyType;
    }

    public DesignatedMembersDetails getDesignatedMembersDetails() {
        return designatedMembersDetails;
    }

    public void setDesignatedMembersDetails(DesignatedMembersDetails designatedMembersDetails) {
        this.designatedMembersDetails = designatedMembersDetails;
    }

    public MembersDetails getMembersDetails() {
        return membersDetails;
    }

    public void setMembersDetails(MembersDetails membersDetails) {
        this.membersDetails = membersDetails;
    }

    public GeneralPartnerDetails getGeneralPartnerDetails() {
        return generalPartnerDetails;
    }

    public void setGeneralPartnerDetails(GeneralPartnerDetails generalPartnerDetails) {
        this.generalPartnerDetails = generalPartnerDetails;
    }

    public LimitedPartnerDetails getLimitedPartnerDetails() {
        return limitedPartnerDetails;
    }

    public void setLimitedPartnerDetails(LimitedPartnerDetails limitedPartnerDetails) {
        this.limitedPartnerDetails = limitedPartnerDetails;
    }

    public PrincipalPlaceOfBusinessDetails getPrincipalPlaceOfBusinessDetails() {
        return principalPlaceOfBusinessDetails;
    }

    public void setPrincipalPlaceOfBusinessDetails(PrincipalPlaceOfBusinessDetails principalPlaceOfBusinessDetails) {
        this.principalPlaceOfBusinessDetails = principalPlaceOfBusinessDetails;
    }

    public Boolean getIncludeGeneralNatureOfBusinessInformation() {
        return includeGeneralNatureOfBusinessInformation;
    }

    public void setIncludeGeneralNatureOfBusinessInformation(Boolean includeGeneralNatureOfBusinessInformation) {
        this.includeGeneralNatureOfBusinessInformation = includeGeneralNatureOfBusinessInformation;
    }

    public LiquidatorsDetails getLiquidatorsDetails() {
        return liquidatorsDetails;
    }

    public void setLiquidatorsDetails(LiquidatorsDetails liquidatorsDetails) {
        this.liquidatorsDetails = liquidatorsDetails;
    }

    public AdministratorsDetails getAdministratorsDetails() {
        return administratorsDetails;
    }

    public void setAdministratorsDetails(AdministratorsDetails administratorsDetails) {
        this.administratorsDetails = administratorsDetails;
    }

    public CompanyStatus getCompanyStatus() {
        return companyStatus;
    }

    public void setCompanyStatus(CompanyStatus companyStatus) {
        this.companyStatus = companyStatus;
    }
}
