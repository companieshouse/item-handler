package uk.gov.companieshouse.itemhandler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Configuration
@PropertySource("classpath:application.properties")
@ConfigurationProperties(prefix = "email")
@Component
public class EmailConfig {
    private String standardCertificateSubjectLine;
    private String expressCertificateSubjectLine;
    private String senderEmail;

    public String getStandardCertificateSubjectLine() {
        return standardCertificateSubjectLine;
    }

    public void setStandardCertificateSubjectLine(String standardCertificateSubjectLine) {
        this.standardCertificateSubjectLine = standardCertificateSubjectLine;
    }

    public String getExpressCertificateSubjectLine() {
        return expressCertificateSubjectLine;
    }

    public void setExpressCertificateSubjectLine(String expressCertificateSubjectLine) {
        this.expressCertificateSubjectLine = expressCertificateSubjectLine;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }
}
