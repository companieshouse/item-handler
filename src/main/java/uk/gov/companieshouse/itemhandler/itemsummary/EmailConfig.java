package uk.gov.companieshouse.itemhandler.itemsummary;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Configuration
@PropertySource("classpath:application.properties")
@ConfigurationProperties(prefix = "email")
@Component
public class EmailConfig {

    private CertificateEmailConfig certificate;
    private CertifiedCopyEmailConfig certifiedCopy;
    private String senderEmail;
    private String ordersAdminHost;

    public CertificateEmailConfig getCertificate() {
        return certificate;
    }

    public void setCertificate(CertificateEmailConfig certificate) {
        this.certificate = certificate;
    }

    public CertifiedCopyEmailConfig getCertifiedCopy() {
        return certifiedCopy;
    }

    public void setCertifiedCopy(CertifiedCopyEmailConfig certifiedCopy) {
        this.certifiedCopy = certifiedCopy;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getOrdersAdminHost() {
        return ordersAdminHost;
    }

    public void setOrdersAdminHost(String ordersAdminHost) {
        this.ordersAdminHost = ordersAdminHost;
    }
}
