package uk.gov.companieshouse.itemhandler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.companieshouse.kafka.deserialization.DeserializerFactory;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;

@Configuration
public class ApplicationConfig implements WebMvcConfigurer {
    @Bean
    DeserializerFactory deserializerFactory() {
        return new DeserializerFactory();
    }

    @Bean
    SerializerFactory serializerFactory() {
        return new SerializerFactory();
    }
}
