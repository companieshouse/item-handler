package uk.gov.companieshouse.itemhandler.config;

import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.itemhandler.client.ApiClient;

@Configuration
public class InternalApiConfig {


    private final ApiClient apiClient;

    public InternalApiConfig (ApiClient apiClient){
        this.apiClient = apiClient;
    }

    @Bean("internalApiClient")
    Supplier<InternalApiClient> internalApiClientSupplier(@Value("${chs.kafka.api.url}") final String chsKafkaApiUrl) {
        return () -> {
            InternalApiClient internalApiClient = apiClient.getInternalApiClient();
            internalApiClient.setBasePath(chsKafkaApiUrl);

            return internalApiClient;
        };
    }

}
