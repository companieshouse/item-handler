package uk.gov.companieshouse.itemhandler.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import email.email_send;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.JsonBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.utility.DockerImageName;
import uk.gov.companieshouse.itemhandler.config.EmbeddedKafkaBrokerConfiguration;
import uk.gov.companieshouse.itemhandler.config.TestEnvironmentSetupHelper;
import uk.gov.companieshouse.itemhandler.service.EmailService;
import uk.gov.companieshouse.itemhandler.util.TestConstants;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.orders.OrderReceived;
import uk.gov.companieshouse.orders.items.ChdItemOrdered;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@Import(EmbeddedKafkaBrokerConfiguration.class)
@TestPropertySource(locations = "classpath:application.properties",
        properties={"uk.gov.companieshouse.item-handler.error-consumer=false"})
class OrderMessageDefaultConsumerIntegrationTest {

    private static int orderId = 123456;
    private static MockServerContainer container;
    private MockServerClient client;

    private static final String DELIVERY_DETAILS_COMPANY_NAME = "Synergia";

    @Autowired
    private KafkaConsumer<String, email_send> emailSendConsumer;

    @Autowired
    private KafkaConsumer<String, ChdItemOrdered> chsItemOrderedConsumer;

    @Autowired
    private KafkaConsumer<String, email_send> itemGroupOrderedConsumer;

    @Autowired
    private KafkaProducer<String, OrderReceived> orderReceivedProducer;

    @Autowired
    private KafkaTopics kafkaTopics;

    @SpyBean
    private OrderProcessResponseHandler orderProcessResponseHandler;

    @SpyBean
    private Logger logger;

    @Captor
    private ArgumentCaptor<String> argumentCaptor;

    @Autowired
    private OrderMessageDefaultConsumerAspect orderMessageDefaultConsumerAspect;

    @BeforeAll
    static void before() {
        container = new MockServerContainer(DockerImageName.parse(
                "jamesdbloom/mockserver:mockserver-5.5.4"));
        container.start();
        TestEnvironmentSetupHelper.setEnvironmentVariable("API_URL",
                "http://" + container.getHost() + ":" + container.getServerPort());
        TestEnvironmentSetupHelper.setEnvironmentVariable("CHS_API_KEY", "123");
        TestEnvironmentSetupHelper.setEnvironmentVariable("PAYMENTS_API_URL",
                "http://" + container.getHost() + ":" + container.getServerPort());
    }

    @AfterAll
    static void after() {
        container.stop();
    }

    @BeforeEach
    void setup() {
        client = new MockServerClient(container.getHost(), container.getServerPort());
    }

    @AfterEach
    void teardown() {
        client.reset();
        ++orderId;
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("certificateTestParameters")
    @DisplayName("Process an order containing certified certificates")
    void testConsumesCertificateOrderReceivedFromEmailSendTopic(String fixture, String description) throws ExecutionException, InterruptedException, IOException {
        //given
        client.when(request()
                        .withPath(getOrderReference())
                        .withMethod(HttpMethod.GET.toString()))
                .respond(response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withBody(JsonBody.json(IOUtils.resourceToString(fixture,
                                StandardCharsets.UTF_8))));
        orderMessageDefaultConsumerAspect.setAfterProcessOrderReceivedEventLatch(new CountDownLatch(1));

        // when
        orderReceivedProducer.send(new ProducerRecord<>(
                kafkaTopics.getOrderReceived(),
                kafkaTopics.getOrderReceived(),
                getOrderReceived())).get();
        orderMessageDefaultConsumerAspect.getAfterProcessOrderReceivedEventLatch().await(30, TimeUnit.SECONDS);
        email_send actual = KafkaTestUtils.getSingleRecord(emailSendConsumer, kafkaTopics.getEmailSend()).value();

        // then
        assertEquals(0, orderMessageDefaultConsumerAspect.getAfterProcessOrderReceivedEventLatch().getCount());
        assertEquals(TestConstants.CERTIFICATE_ORDER_NOTIFICATION_API_APP_ID, actual.getAppId());
        assertNotNull(actual.getMessageId());
        assertEquals(TestConstants.CERTIFICATE_ORDER_NOTIFICATION_API_MESSAGE_TYPE,
                actual.getMessageType());
        assertEquals(EmailService.TOKEN_EMAIL_ADDRESS, actual.getEmailAddress());
        assertNotNull(actual.getData());

        final JsonNode data = new ObjectMapper().readTree(actual.getData());
        final String companyName = (data.get("delivery_details") != null &&
                                    data.get("delivery_details").findValue("company_name") != null) ?
                data.get("delivery_details").findValue("company_name").textValue() : "";
        assertEquals(DELIVERY_DETAILS_COMPANY_NAME, companyName);
    }

    @Test
    @DisplayName("Process an order containing a single digital certificate")
    void testDigitalCertificateResultsInItemGroupOrderedMessage() throws ExecutionException, InterruptedException, IOException {
        //given
        client.when(request()
                        .withPath(getOrderReference())
                        .withMethod(HttpMethod.GET.toString()))
                .respond(response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withBody(JsonBody.json(IOUtils.resourceToString("/fixtures/digital-certificate.json",
                                StandardCharsets.UTF_8))));
        orderMessageDefaultConsumerAspect.setAfterProcessOrderReceivedEventLatch(new CountDownLatch(1));

        // when
        orderReceivedProducer.send(new ProducerRecord<>(
                kafkaTopics.getOrderReceived(),
                kafkaTopics.getOrderReceived(),
                getOrderReceived())).get();
        orderMessageDefaultConsumerAspect.getAfterProcessOrderReceivedEventLatch().await(30, TimeUnit.SECONDS);
        email_send itemGroupOrdered = KafkaTestUtils.getSingleRecord(itemGroupOrderedConsumer,
                kafkaTopics.getItemGroupOrdered()).value();

        // then
        assertEquals(0, orderMessageDefaultConsumerAspect.getAfterProcessOrderReceivedEventLatch().getCount());
        assertEquals("item-handler", itemGroupOrdered.getAppId());
        assertNotNull(itemGroupOrdered.getMessageId());
        assertEquals("TBD", itemGroupOrdered.getMessageType());
        assertEquals("unknown@unknown.com", itemGroupOrdered.getEmailAddress());
        assertNotNull(itemGroupOrdered.getData());

// TODO DCAC-264 - what should we check here?
//        final JsonNode data = new ObjectMapper().readTree(itemGroupOrdered.getData());
//        final String companyName = (data.get("delivery_details") != null &&
//                data.get("delivery_details").findValue("company_name") != null) ?
//                data.get("delivery_details").findValue("company_name").textValue() : "";
//        assertEquals(DELIVERY_DETAILS_COMPANY_NAME, companyName);
    }

    @Test
    @DisplayName("Process an order containing certified certificates with different delivery timescales")
    void testConsumesCertsOrderWithDifferentDeliveryTimescalesReceivedFromEmailSendTopic() throws ExecutionException, InterruptedException, IOException {
        //given
        client.when(request()
                        .withPath(getOrderReference())
                        .withMethod(HttpMethod.GET.toString()))
                .respond(response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withBody(JsonBody.json(IOUtils.resourceToString("/fixtures/multi-certified-certificate-timescales.json",
                                StandardCharsets.UTF_8))));
        orderMessageDefaultConsumerAspect.setAfterProcessOrderReceivedEventLatch(new CountDownLatch(1));

        // when
        orderReceivedProducer.send(new ProducerRecord<>(
                kafkaTopics.getOrderReceived(),
                kafkaTopics.getOrderReceived(),
                getOrderReceived())).get();
        orderMessageDefaultConsumerAspect.getAfterProcessOrderReceivedEventLatch().await(30, TimeUnit.SECONDS);
        ConsumerRecords<String, email_send> actual = KafkaTestUtils.getRecords(emailSendConsumer, 30000L, 2);

        // then
        assertEquals(0, orderMessageDefaultConsumerAspect.getAfterProcessOrderReceivedEventLatch().getCount());
        assertEquals(2, actual.count());
        for(ConsumerRecord<String, email_send> record : actual) {
            assertEquals(TestConstants.CERTIFICATE_ORDER_NOTIFICATION_API_APP_ID, record.value().getAppId());
            assertNotNull(record.value().getMessageId());
            assertEquals(TestConstants.CERTIFICATE_ORDER_NOTIFICATION_API_MESSAGE_TYPE,
                    record.value().getMessageType());
            assertEquals(EmailService.TOKEN_EMAIL_ADDRESS, record.value().getEmailAddress());
            assertNotNull(record.value().getData());
        }
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("certifiedCopyTestParameters")
    @DisplayName("Process an order containing certified copies")
    void testConsumesCertifiedCopyOrderReceivedFromEmailSendTopic(String fixture, String description) throws ExecutionException, InterruptedException, IOException {
        //given
        client.when(request()
                .withPath(getOrderReference())
                .withMethod(HttpMethod.GET.toString()))
                .respond(response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withBody(JsonBody.json(IOUtils.resourceToString(fixture,
                                StandardCharsets.UTF_8))));
        orderMessageDefaultConsumerAspect.setAfterProcessOrderReceivedEventLatch(new CountDownLatch(1));

        // when
        orderReceivedProducer.send(new ProducerRecord<>(
                kafkaTopics.getOrderReceived(),
                kafkaTopics.getOrderReceived(),
                getOrderReceived())).get();
        orderMessageDefaultConsumerAspect.getAfterProcessOrderReceivedEventLatch().await(30, TimeUnit.SECONDS);
        email_send actual = KafkaTestUtils.getSingleRecord(emailSendConsumer, kafkaTopics.getEmailSend()).value();

        // then
        assertEquals(0, orderMessageDefaultConsumerAspect.getAfterProcessOrderReceivedEventLatch().getCount());
        assertEquals(TestConstants.CERTIFIED_COPY_ORDER_NOTIFICATION_API_APP_ID, actual.getAppId());
        assertNotNull(actual.getMessageId());
        assertEquals(TestConstants.CERTIFIED_COPY_ORDER_NOTIFICATION_API_MESSAGE_TYPE,
                actual.getMessageType());
        assertEquals(EmailService.TOKEN_EMAIL_ADDRESS, actual.getEmailAddress());
        assertNotNull(actual.getData());
    }

    @Test
    @DisplayName("Process an order containing a single digital copy")
    void testDigitalCopyResultsInItemGroupOrderedMessage() throws ExecutionException, InterruptedException, IOException {
        //given
        client.when(request()
                        .withPath(getOrderReference())
                        .withMethod(HttpMethod.GET.toString()))
                .respond(response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withBody(JsonBody.json(IOUtils.resourceToString("/fixtures/digital-copy.json",
                                StandardCharsets.UTF_8))));
        orderMessageDefaultConsumerAspect.setAfterProcessOrderReceivedEventLatch(new CountDownLatch(1));

        // when
        orderReceivedProducer.send(new ProducerRecord<>(
                kafkaTopics.getOrderReceived(),
                kafkaTopics.getOrderReceived(),
                getOrderReceived())).get();
        orderMessageDefaultConsumerAspect.getAfterProcessOrderReceivedEventLatch().await(30, TimeUnit.SECONDS);
        email_send itemGroupOrdered = KafkaTestUtils.getSingleRecord(itemGroupOrderedConsumer,
                kafkaTopics.getItemGroupOrdered()).value();

        // then
        assertEquals(0, orderMessageDefaultConsumerAspect.getAfterProcessOrderReceivedEventLatch().getCount());
        assertEquals("item-handler", itemGroupOrdered.getAppId());
        assertNotNull(itemGroupOrdered.getMessageId());
        assertEquals("TBD", itemGroupOrdered.getMessageType());
        assertEquals("unknown@unknown.com", itemGroupOrdered.getEmailAddress());
        assertNotNull(itemGroupOrdered.getData());

// TODO DCAC-264 - what should we check here?
//        final JsonNode data = new ObjectMapper().readTree(itemGroupOrdered.getData());
//        final String companyName = (data.get("delivery_details") != null &&
//                data.get("delivery_details").findValue("company_name") != null) ?
//                data.get("delivery_details").findValue("company_name").textValue() : "";
//        assertEquals(DELIVERY_DETAILS_COMPANY_NAME, companyName);
    }

    @Test
    @DisplayName("Process an order containing certified copies with different delivery timescales")
    void testConsumesCertCopiesOrderWithDifferentDeliveryTimescalesReceivedFromEmailSendTopic() throws ExecutionException, InterruptedException, IOException {
        //given
        client.when(request()
                .withPath(getOrderReference())
                .withMethod(HttpMethod.GET.toString()))
                .respond(response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withBody(JsonBody.json(IOUtils.resourceToString("/fixtures/multi-certified-copy-timescales.json",
                                StandardCharsets.UTF_8))));
        orderMessageDefaultConsumerAspect.setAfterProcessOrderReceivedEventLatch(new CountDownLatch(1));

        // when
        orderReceivedProducer.send(new ProducerRecord<>(
                kafkaTopics.getOrderReceived(),
                kafkaTopics.getOrderReceived(),
                getOrderReceived())).get();
        orderMessageDefaultConsumerAspect.getAfterProcessOrderReceivedEventLatch().await(30, TimeUnit.SECONDS);
        ConsumerRecords<String, email_send> actual = KafkaTestUtils.getRecords(emailSendConsumer, 30000L, 2);

        // then
        assertEquals(0, orderMessageDefaultConsumerAspect.getAfterProcessOrderReceivedEventLatch().getCount());
        assertEquals(2, actual.count());
        for(ConsumerRecord<String, email_send> record : actual) {
            assertEquals(TestConstants.CERTIFIED_COPY_ORDER_NOTIFICATION_API_APP_ID, record.value().getAppId());
            assertNotNull(record.value().getMessageId());
            assertEquals(TestConstants.CERTIFIED_COPY_ORDER_NOTIFICATION_API_MESSAGE_TYPE,
                    record.value().getMessageType());
            assertEquals(EmailService.TOKEN_EMAIL_ADDRESS, record.value().getEmailAddress());
            assertNotNull(record.value().getData());
        }
    }

    @Test
    void testConsumesMissingImageDeliveryFromOrderReceivedAndPublishesChsItemOrdered() throws ExecutionException, InterruptedException, IOException {
        // given
        client.when(request()
                        .withPath(getOrderReference())
                        .withMethod(HttpMethod.GET.toString()))
                .respond(response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, "application/json")
                        .withBody(JsonBody.json(IOUtils.resourceToString(
                                "/fixtures/missing-image-delivery.json",
                                StandardCharsets.UTF_8))));
        orderMessageDefaultConsumerAspect.setAfterProcessOrderReceivedEventLatch(new CountDownLatch(1));

        // when
        orderReceivedProducer.send(new ProducerRecord<>(kafkaTopics.getOrderReceived(),
                kafkaTopics.getOrderReceived(),
                getOrderReceived())).get();
        orderMessageDefaultConsumerAspect.getAfterProcessOrderReceivedEventLatch().await(30, TimeUnit.SECONDS);
        ChdItemOrdered actual = KafkaTestUtils.getSingleRecord(chsItemOrderedConsumer, kafkaTopics.getChdItemOrdered()).value();

        // then
        assertEquals(0, orderMessageDefaultConsumerAspect.getAfterProcessOrderReceivedEventLatch().getCount());
        assertEquals("ORD-123123-123123", actual.getReference());
        assertNotNull(actual.getItem());
    }

    @Test
    @DisplayName("Should successfully process an order containing multiple missing image delivery items")
    void testConsumesMultipleMissingImageDeliveriesFromOrderReceivedAndPublishesChsItemOrderedTwice() throws ExecutionException, InterruptedException, IOException {
        // given
        int midId = 123123;
        client.when(request()
                .withPath(getOrderReference())
                .withMethod(HttpMethod.GET.toString()))
                .respond(response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, "application/json")
                        .withBody(JsonBody.json(IOUtils.resourceToString(
                                "/fixtures/multiple-missing-image-delivery.json",
                                StandardCharsets.UTF_8))));
        orderMessageDefaultConsumerAspect.setAfterProcessOrderReceivedEventLatch(new CountDownLatch(1));

        // when
        orderReceivedProducer.send(new ProducerRecord<>(kafkaTopics.getOrderReceived(),
                kafkaTopics.getOrderReceived(),
                getOrderReceived())).get();
        orderMessageDefaultConsumerAspect.getAfterProcessOrderReceivedEventLatch().await(30, TimeUnit.SECONDS);
        ConsumerRecords<String, ChdItemOrdered> actual = KafkaTestUtils.getRecords(chsItemOrderedConsumer, 30000L, 2);

        // then
        assertEquals(0, orderMessageDefaultConsumerAspect.getAfterProcessOrderReceivedEventLatch().getCount());
        assertEquals(2, actual.count());
        for(ConsumerRecord<String, ChdItemOrdered> record : actual) {
            assertEquals("ORD-123123-123123", record.value().getReference());
            assertNotNull(record.value().getItem());
            assertEquals("MID-123123-" + midId++, record.value().getItem().getId());
        }
    }

    @Test
    void testLogAnErrorWhenOrdersApiReturnsOrderNotFound() throws ExecutionException, InterruptedException {
        //given
        client.when(request()
                        .withPath(getOrderReference())
                        .withMethod(HttpMethod.GET.toString()))
                .respond(response()
                        .withStatusCode(HttpStatus.NOT_FOUND.value()));
        orderMessageDefaultConsumerAspect.setAfterProcessOrderReceivedEventLatch(new CountDownLatch(1));

        // when
        orderReceivedProducer.send(new ProducerRecord<>(
                kafkaTopics.getOrderReceived(),
                kafkaTopics.getOrderReceived(),
                getOrderReceived())).get();
        orderMessageDefaultConsumerAspect.getAfterProcessOrderReceivedEventLatch().await(30, TimeUnit.SECONDS);

        // then
        assertEquals(0, orderMessageDefaultConsumerAspect.getAfterProcessOrderReceivedEventLatch().getCount());
        verify(orderProcessResponseHandler).serviceError(any());
        verify(logger).error(argumentCaptor.capture(), anyMap());
        assertEquals("order-received message processing failed with a "
                + "non-recoverable exception", argumentCaptor.getValue());
    }

    private OrderReceived getOrderReceived() {
        OrderReceived orderReceived = new OrderReceived();
        orderReceived.setOrderUri(getOrderReference());
        return orderReceived;
    }

    private String getOrderReference() {
        return "/orders/ORD-111111-" + orderId;
    }

    private static Stream<Arguments> certificateTestParameters() {
        return Stream.of(
                Arguments.of("/fixtures/certified-certificate.json", "Order containing one certificate"),
                Arguments.of("/fixtures/multi-certified-certificate.json", "Order containing multiple certificates")
        );
    }

    private static Stream<Arguments> certifiedCopyTestParameters() {
        return Stream.of(
                Arguments.of("/fixtures/certified-copy.json", "Order containing one certified copy"),
                Arguments.of("/fixtures/multi-certified-copy.json", "Order containing multiple certified copies")
        );
    }
}