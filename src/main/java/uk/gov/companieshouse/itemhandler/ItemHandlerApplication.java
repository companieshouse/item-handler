package uk.gov.companieshouse.itemhandler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import uk.gov.companieshouse.api.model.order.OrdersApi;
import uk.gov.companieshouse.itemhandler.mapper.OrdersApiToOrderDataMapper;
import uk.gov.companieshouse.itemhandler.model.OrderData;

@SpringBootApplication
public class ItemHandlerApplication {

	public static final String APPLICATION_NAMESPACE = "item-handler";
	public static final String LOG_MESSAGE_DATA_KEY = "message";

	public static void main(String[] args) {
		SpringApplication.run(ItemHandlerApplication.class, args);
	}

}
