package uk.gov.companieshouse.itemhandler.service;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.companieshouse.itemhandler.client.ApiClient;
import uk.gov.companieshouse.itemhandler.mapper.OrdersApiToOrderDataMapper;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Partially Unit/integration tests the {@link OrdersApiClientService} class. Uses JUnit4 to take advantage of the
 * system-rules {@link EnvironmentVariables} class rule. The JUnit5 system-extensions equivalent does not
 * seem to have been released.
 */
@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
@SpringJUnitConfig(OrdersApiClientServiceIntegrationTest.Config.class)
@AutoConfigureWireMock(port = 0)
public class OrdersApiClientServiceIntegrationTest {

    private static final String ORDER_URL = "/orders/1234";

    @ClassRule
    public static final EnvironmentVariables ENVIRONMENT_VARIABLES = new EnvironmentVariables();

    @Configuration
    @ComponentScan(basePackageClasses = OrdersApiClientServiceIntegrationTest.class)
    static class Config {
        @Bean
        ApiClient getApiClient() {
            return new ApiClient();
        }
    }

    @Autowired
    private OrdersApiClientService serviceUnderTest;

    @Autowired
    private  ApiClient apiClient;

    @Autowired
    private Environment environment;

    @MockBean
    private OrdersApiToOrderDataMapper ordersApiToOrderDataMapper;

    // TODO GCI-1182 Why?
    @MockBean
    private EmailService emailService;

//    @MockBean
//    private OrdersKafkaConsumerWrapper consumerWrapper;

    @Test
    public void redirectsReportedAsErrors() throws Exception {

        final String wireMockPort = environment.getProperty("wiremock.server.port");

        ENVIRONMENT_VARIABLES.set("CHS_API_KEY", "MGQ1MGNlYmFkYzkxZTM2MzlkNGVmMzg4ZjgxMmEz");
        ENVIRONMENT_VARIABLES.set("API_URL", "http://localhost:" + wireMockPort);
        ENVIRONMENT_VARIABLES.set("PAYMENTS_API_URL", "blah");
        givenThat(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("blah"))
                .willReturn(temporaryRedirect("somewhere else")
                        .withHeader("Content-Type", "application/json")));
                        /*.withBody(objectMapper.writeValueAsString(COMPANY_NOT_FOUND))));*/

        serviceUnderTest.getOrderData(ORDER_URL);

    }

}
