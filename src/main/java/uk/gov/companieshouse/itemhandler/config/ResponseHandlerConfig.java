package uk.gov.companieshouse.itemhandler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Configuration
@PropertySource("classpath:application.properties")
@ConfigurationProperties(prefix = "response.handler")
@Component
public class ResponseHandlerConfig {

    private int maximumRetryAttempts;
    private String retryTopic;
    private String errorTopic;

    public int getMaximumRetryAttempts() {
        return maximumRetryAttempts;
    }

    public void setMaximumRetryAttempts(int maximumRetryAttempts) {
        this.maximumRetryAttempts = maximumRetryAttempts;
    }

    public String getRetryTopic() {
        return retryTopic;
    }

    public void setRetryTopic(String retryTopic) {
        this.retryTopic = retryTopic;
    }

    public String getErrorTopic() {
        return errorTopic;
    }

    public void setErrorTopic(String errorTopic) {
        this.errorTopic = errorTopic;
    }

}
