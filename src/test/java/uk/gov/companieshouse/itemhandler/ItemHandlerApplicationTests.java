package uk.gov.companieshouse.itemhandler;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import uk.gov.companieshouse.kafka.deserialization.DeserializerFactory;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka
class ItemHandlerApplicationTests {

	@Test
	void contextLoads() {
	}

	@Bean
	DeserializerFactory deserializerFactory() {
		return new DeserializerFactory();
	}

	@Bean
	SerializerFactory serializerFactory() {
		return new SerializerFactory();
	}
}
