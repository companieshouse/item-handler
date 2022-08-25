package uk.gov.companieshouse.itemhandler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Configuration
@PropertySource("classpath:application.properties")
@ConfigurationProperties("email")
@Component
public class EmailConfig {
    private final String standardCertificateSubjectLine;
    private final String expressCertificateSubjectLine;
    private final String senderEmail;

    public EmailConfig(String standardCertificateSubjectLine, String expressCertificateSubjectLine, String senderEmail) {
        this.standardCertificateSubjectLine = standardCertificateSubjectLine;
        this.expressCertificateSubjectLine = expressCertificateSubjectLine;
        this.senderEmail = senderEmail;
    }

    public String getStandardCertificateSubjectLine() {
        return standardCertificateSubjectLine;
    }

    public String getExpressCertificateSubjectLine() {
        return expressCertificateSubjectLine;
    }

    public String getSenderEmail() {
        return senderEmail;
    }
}
