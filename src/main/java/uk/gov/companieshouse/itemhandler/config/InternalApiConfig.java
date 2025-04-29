package uk.gov.companieshouse.itemhandler.config;

import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.http.ApiKeyHttpClient;

@Configuration
public class InternalApiConfig {

    @Bean("internalApiClient")
    Supplier<InternalApiClient> internalApiClientSupplier(@Value("${chs.kafka.api.url}") final String chsKafkaApiUrl) {
        return () -> {
            InternalApiClient internalApiClient = new InternalApiClient(new ApiKeyHttpClient(""));
            internalApiClient.setBasePath(chsKafkaApiUrl);

            return internalApiClient;
        };
    }

}
