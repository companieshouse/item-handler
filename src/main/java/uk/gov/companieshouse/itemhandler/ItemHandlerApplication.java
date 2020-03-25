package uk.gov.companieshouse.itemhandler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ItemHandlerApplication {

	public static final String APPLICATION_NAMESPACE = "orders.api.ch.gov.uk";

	public static void main(String[] args) {
		SpringApplication.run(ItemHandlerApplication.class, args);
	}

}
