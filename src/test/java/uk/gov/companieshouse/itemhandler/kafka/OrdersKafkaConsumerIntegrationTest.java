package uk.gov.companieshouse.itemhandler.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.env.Environment;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.companieshouse.api.model.order.OrdersApi;
import uk.gov.companieshouse.kafka.consumer.resilience.CHConsumerType;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Partially Unit/integration tests the {@link OrdersKafkaConsumer} class. Uses JUnit4 to take advantage of the
 * system-rules {@link EnvironmentVariables} class rule. The JUnit5 system-extensions equivalent does not
 * seem to have been released.
 */
@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
@EmbeddedKafka
@TestPropertySource(properties={"uk.gov.companieshouse.item-handler.error-consumer=false",
                                "certificate.order.confirmation.recipient = nobody@nowhere.com"})
@AutoConfigureWireMock(port = 0)
public class OrdersKafkaConsumerIntegrationTest {
    private static final String ORDER_RECEIVED_TOPIC = "order-received";

    // TODO GCI-1182 Use or lose.
    private static final String ORDER_RECEIVED_TOPIC_RETRY = "order-received-retry";
    private static final String ORDER_RECEIVED_TOPIC_ERROR = "order-received-error";

    private static final String ORDER_RECEIVED_URI = "/orders/ORDER-12345";
    private static final String ORDER_RECEIVED_MESSAGE_JSON = "{\"order_uri\": \"/orders/ORDER-12345\"}";
    private static final OrdersApi ORDER = new OrdersApi();

    @ClassRule
    public static final EnvironmentVariables ENVIRONMENT_VARIABLES = new EnvironmentVariables();

    @Autowired
    private OrdersKafkaProducer kafkaProducer;

    @Autowired
    private OrdersKafkaConsumerWrapper consumerWrapper;

    @Autowired
    private Environment environment;

    @Autowired
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        final String wireMockPort = environment.getProperty("wiremock.server.port");
        ENVIRONMENT_VARIABLES.set("CHS_API_KEY", "MGQ1MGNlYmFkYzkxZTM2MzlkNGVmMzg4ZjgxMmEz");
        ENVIRONMENT_VARIABLES.set("API_URL", "http://localhost:" + wireMockPort);
        ENVIRONMENT_VARIABLES.set("PAYMENTS_API_URL", "blah");
    }

    @After
    public void tearDown() {
        consumerWrapper.reset();
        reset();
    }

// TODO GCI-1182 Use or lose.
//    @Test
//    @DisplayName("order-received-error topic consumer does not receive message when 'error-consumer' (env var IS_ERROR_QUEUE_CONSUMER) is false")
//    public void testOrdersConsumerReceivesOrderReceivedMessage1Error() throws InterruptedException, ExecutionException, SerializationException {
//        // When
//        kafkaProducer.sendMessage(consumerWrapper.createMessage(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC_ERROR));
//
//        // Then
//        verifyProcessOrderReceivedNotInvoked(CHConsumerType.ERROR_CONSUMER);
//    }
//
//    private void verifyProcessOrderReceivedNotInvoked(CHConsumerType type) throws InterruptedException {
//        consumerWrapper.setTestType(type);
//        consumerWrapper.getLatch().await(3000, TimeUnit.MILLISECONDS);
//        assertThat(consumerWrapper.getLatch().getCount(), is(equalTo(1L)));
//        String processedOrderUri = consumerWrapper.getOrderUri();
//        assertThat(processedOrderUri, isEmptyOrNullString());
//    }

    @Test
    public void fairWeather() throws InterruptedException, ExecutionException, SerializationException, JsonProcessingException {

        // Given
        givenThat(get(urlEqualTo(ORDER_RECEIVED_URI))
                .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(ORDER))));

        // When
        kafkaProducer.sendMessage(consumerWrapper.createMessage(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC));

        // Then
        verifyProcessOrderReceivedInvoked(CHConsumerType.MAIN_CONSUMER);
        verify(1, getRequestedFor(urlEqualTo(ORDER_RECEIVED_URI)));
    }

    @Test
    public void fourXxIsNotRetryable() throws InterruptedException, ExecutionException, SerializationException {

        // Given
        givenThat(get(urlEqualTo(ORDER_RECEIVED_URI))
                .willReturn(notFound()
                .withHeader("Content-Type", "application/json")));

        // When
        kafkaProducer.sendMessage(consumerWrapper.createMessage(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC));

        // Then
        verifyProcessOrderReceivedInvoked(CHConsumerType.MAIN_CONSUMER);
        verify(1, getRequestedFor(urlEqualTo(ORDER_RECEIVED_URI)));
    }

    @Test
    public void fiveXxIsRetryable() throws InterruptedException, ExecutionException, SerializationException, JsonProcessingException {

        // Given
        // The initial request is rejected due to the Orders API hitting a Server Internal Error
        givenThat(get(urlEqualTo(ORDER_RECEIVED_URI))
                .inScenario("5xx retry")
                .whenScenarioStateIs(STARTED)
                .willSetStateTo("Initial request rejected")
                .willReturn(serverError()
                .withHeader("Content-Type", "application/json")));

        // and the subsequent request is responded to correctly as the Orders API has recovered
        givenThat(get(urlEqualTo(ORDER_RECEIVED_URI))
                .inScenario("5xx retry")
                .whenScenarioStateIs("Initial request rejected")
                .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(ORDER))));

        // When
        kafkaProducer.sendMessage(consumerWrapper.createMessage(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC));

        // Then
        verifyProcessOrderReceivedInvoked(CHConsumerType.MAIN_CONSUMER);
        // TODO GCI-1182 Give scenario time to play out?
        Thread.sleep(100);
        verify(2, getRequestedFor(urlEqualTo(ORDER_RECEIVED_URI)));
    }

// TODO GCI-1182 Use or lose.
//    @Test
//    @DisplayName("order-received-retry topic consumer receives message when 'error-consumer' (env var IS_ERROR_QUEUE_CONSUMER) is false")
//    public void testOrdersConsumerReceivesOrderReceivedMessage3Retry() throws InterruptedException, ExecutionException, SerializationException {
//        // When
//        kafkaProducer.sendMessage(consumerWrapper.createMessage(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC_RETRY));
//
//        // Then
//        verifyProcessOrderReceivedInvoked(CHConsumerType.RETRY_CONSUMER);
//    }

    private void verifyProcessOrderReceivedInvoked(CHConsumerType type) throws InterruptedException {
        consumerWrapper.setTestType(type);
        consumerWrapper.getLatch().await(3000, TimeUnit.MILLISECONDS);
        assertThat(consumerWrapper.getLatch().getCount(), is(equalTo(0L)));
        String processedOrderUri = consumerWrapper.getOrderUri();
        assertThat(processedOrderUri, is(equalTo(ORDER_RECEIVED_MESSAGE_JSON)));
    }
}
