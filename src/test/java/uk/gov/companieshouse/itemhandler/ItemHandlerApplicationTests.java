package uk.gov.companieshouse.itemhandler;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka
@TestPropertySource(properties="certificate.order.confirmation.recipients = nobody@nowhere.com")
class ItemHandlerApplicationTests {

	@Test
	void contextLoads() {
	}

}
