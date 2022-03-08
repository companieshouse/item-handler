package uk.gov.companieshouse.itemhandler.email;

import uk.gov.companieshouse.itemhandler.config.FeatureOptions;
import uk.gov.companieshouse.itemhandler.model.Content;

/**
 * An instance of this holds the information required to generate a certificate order confirmation email.
 */
public class CertificateOrderConfirmation extends OrderConfirmation {

    private String feeAmount;
    private String certificateType;
    private String[] certificateIncludes;
    private String deliveryMethod;
    private String certificateGoodStandingInformation;
    private String certificateDirectors;
    private String certificateSecretaries;
    private String certificateCompanyObjects;
    private String[] certificateDirectorOptions;
    private String certificateRegisteredOfficeOptions;
    private String[] certificateSecretaryOptions;
    private String certificateCompanyType;
    private String certificateDesignatedMembers;
    private String[] certificateDesignatedMembersDetails;
    private String certificateMembers;
    private String[] certificateMembersDetails;
    private String certificateGeneralPartner;
    private String certificateLimitedPartner;
    private String certificatePrincipalPlaceOfBusinessDetails;
    private String certificateGeneralNatureOfBusinessInformation;
    private Content<String> certificateLiquidatorsDetails;
    private Content<String> certificateAdministratorsDetails;
    private FeatureOptions featureOptions;

    public String getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(String feeAmount) {
        this.feeAmount = feeAmount;
    }

    public String getCertificateType() {
        return certificateType;
    }

    public void setCertificateType(String certificateType) {
        this.certificateType = certificateType;
    }

    public String[] getCertificateIncludes() {
        return certificateIncludes;
    }

    public void setCertificateIncludes(String[] certificateIncludes) {
        this.certificateIncludes = certificateIncludes;
    }

    public String getCertificateGoodStandingInformation() {
        return certificateGoodStandingInformation;
    }

    public void setCertificateGoodStandingInformation(String certificateGoodStandingInformation) {
        this.certificateGoodStandingInformation = certificateGoodStandingInformation;
    }

    public String getCertificateRegisteredOfficeOptions() {
        return certificateRegisteredOfficeOptions;
    }

    public void setCertificateRegisteredOfficeOptions(String certificateRegisteredOfficeOptions) {
        this.certificateRegisteredOfficeOptions = certificateRegisteredOfficeOptions;
    }

    public String[] getCertificateDirectorOptions() {
        return certificateDirectorOptions;
    }

    public void setCertificateDirectorOptions(String[] certificateDirectorOptions) {
        this.certificateDirectorOptions = certificateDirectorOptions;
    }

    public String[] getCertificateSecretaryOptions() {
        return certificateSecretaryOptions;
    }

    public void setCertificateSecretaryOptions(String[] certificateSecretaryOptions) {
        this.certificateSecretaryOptions = certificateSecretaryOptions;
    }

    public String getCertificateDirectors() {
        return certificateDirectors;
    }

    public void setCertificateDirectors(String certificateDirectors) {
        this.certificateDirectors = certificateDirectors;
    }

    public String getCertificateSecretaries() {
        return certificateSecretaries;
    }

    public void setCertificateSecretaries(String certificateSecretaries) {
        this.certificateSecretaries = certificateSecretaries;
    }

    public String getCertificateCompanyObjects() {
        return certificateCompanyObjects;
    }

    public void setCertificateCompanyObjects(String certificateCompanyObjects) {
        this.certificateCompanyObjects = certificateCompanyObjects;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    public String getCertificateCompanyType() {
        return certificateCompanyType;
    }

    public void setCertificateCompanyType(String certificateCompanyType) {
        this.certificateCompanyType = certificateCompanyType;
    }

    public String getCertificateDesignatedMembers() {
        return certificateDesignatedMembers;
    }

    public void setCertificateDesignatedMembers(String certificateDesignatedMembers) {
        this.certificateDesignatedMembers = certificateDesignatedMembers;
    }

    public String[] getCertificateDesignatedMembersDetails() {
        return certificateDesignatedMembersDetails;
    }

    public void setCertificateDesignatedMembersDetails(String[] certificateDesignatedMembersDetails) {
        this.certificateDesignatedMembersDetails = certificateDesignatedMembersDetails;
    }

    public String getCertificateMembers() {
        return certificateMembers;
    }

    public void setCertificateMembers(String certificateMembers) {
        this.certificateMembers = certificateMembers;
    }

    public String[] getCertificateMembersDetails() {
        return certificateMembersDetails;
    }

    public void setCertificateMembersDetails(String[] certificateMembersDetails) {
        this.certificateMembersDetails = certificateMembersDetails;
    }

    public String getCertificateGeneralPartner() {
        return certificateGeneralPartner;
    }

    public void setCertificateGeneralPartner(String certificateGeneralPartner) {
        this.certificateGeneralPartner = certificateGeneralPartner;
    }

    public String getCertificateLimitedPartner() {
        return certificateLimitedPartner;
    }

    public void setCertificateLimitedPartner(String certificateLimitedPartner) {
        this.certificateLimitedPartner = certificateLimitedPartner;
    }

    public String getCertificatePrincipalPlaceOfBusinessDetails() {
        return certificatePrincipalPlaceOfBusinessDetails;
    }

    public void setCertificatePrincipalPlaceOfBusinessDetails(String certificatePrincipalPlaceOfBusinessDetails) {
        this.certificatePrincipalPlaceOfBusinessDetails = certificatePrincipalPlaceOfBusinessDetails;
    }

    public String getCertificateGeneralNatureOfBusinessInformation() {
        return certificateGeneralNatureOfBusinessInformation;
    }

    public void setCertificateGeneralNatureOfBusinessInformation(String certificateGeneralNatureOfBusinessInformation) {
        this.certificateGeneralNatureOfBusinessInformation = certificateGeneralNatureOfBusinessInformation;
    }

    public Content<String> getCertificateLiquidatorsDetails() {
        return certificateLiquidatorsDetails;
    }

    public void setCertificateLiquidatorsDetails(Content<String> certificateLiquidatorsDetails) {
        this.certificateLiquidatorsDetails = certificateLiquidatorsDetails;
    }

    public Content<String> getCertificateAdministratorsDetails() {
        return certificateAdministratorsDetails;
    }

    public void setCertificateAdministratorsDetails(Content<String> certificateAdministratorsDetails) {
        this.certificateAdministratorsDetails = certificateAdministratorsDetails;
    }

    public FeatureOptions getFeatureOptions() {
        return featureOptions;
    }

    public void setFeatureOptions(FeatureOptions featureOptions) {
        this.featureOptions = featureOptions;
    }
}
