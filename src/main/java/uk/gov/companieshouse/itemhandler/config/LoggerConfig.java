package uk.gov.companieshouse.itemhandler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.logging.Logger;

/**
 * TODO: make Logging utils a Spring Bean
 */
@Configuration
public class LoggerConfig {

    @Bean
    public Logger logger() {
        return LoggingUtils.getLogger();
    }
}
