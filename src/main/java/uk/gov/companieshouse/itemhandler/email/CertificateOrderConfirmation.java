package uk.gov.companieshouse.itemhandler.email;

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
}
