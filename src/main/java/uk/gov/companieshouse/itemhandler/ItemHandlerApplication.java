package uk.gov.companieshouse.itemhandler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ItemHandlerApplication {

	public static final String APPLICATION_NAMESPACE = "item-handler";

	public static void main(String[] args) {
		SpringApplication.run(ItemHandlerApplication.class, args);
	}

}
