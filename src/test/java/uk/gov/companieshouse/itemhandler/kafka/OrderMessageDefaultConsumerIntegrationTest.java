package uk.gov.companieshouse.itemhandler.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import email.email_send;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

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
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.orders.OrderReceived;
import uk.gov.companieshouse.orders.items.ChdItemOrdered;

@SpringBootTest
@Import(EmbeddedKafkaBrokerConfiguration.class)
@TestPropertySource(locations = "classpath:application.properties",
        properties={"uk.gov.companieshouse.item-handler.error-consumer=false"})
class OrderMessageDefaultConsumerIntegrationTest {

    private static int orderId = 123456;
    private static int midId = 123123;
    private static MockServerContainer container;
    private MockServerClient client;

    @Autowired
    private KafkaConsumer<String, email_send> emailSendConsumer;

    @Autowired
    private KafkaConsumer<String, ChdItemOrdered> chsItemOrderedConsumer;

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
        assertEquals(EmailService.CERTIFICATE_ORDER_NOTIFICATION_API_APP_ID, actual.getAppId());
        assertNotNull(actual.getMessageId());
        assertEquals(EmailService.CERTIFICATE_ORDER_NOTIFICATION_API_MESSAGE_TYPE,
                actual.getMessageType());
        assertEquals(EmailService.TOKEN_EMAIL_ADDRESS, actual.getEmailAddress());
        assertNotNull(actual.getData());
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
            assertEquals(EmailService.CERTIFICATE_ORDER_NOTIFICATION_API_APP_ID, record.value().getAppId());
            assertNotNull(record.value().getMessageId());
            assertEquals(EmailService.CERTIFICATE_ORDER_NOTIFICATION_API_MESSAGE_TYPE,
                    record.value().getMessageType());
            assertEquals(EmailService.TOKEN_EMAIL_ADDRESS, record.value().getEmailAddress());
            assertNotNull(record.value().getData());
        }
    }

    @Test
    void testConsumesCertifiedDocumentOrderReceivedFromEmailSendTopic() throws ExecutionException, InterruptedException, IOException {
        // given
        client.when(request()
                        .withPath(getOrderReference())
                        .withMethod(HttpMethod.GET.toString()))
                .respond(response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, "application/json")
                        .withBody(JsonBody.json(IOUtils.resourceToString(
                                "/fixtures/certified-copy.json",
                                StandardCharsets.UTF_8))));
        orderMessageDefaultConsumerAspect.setAfterProcessOrderReceivedEventLatch(new CountDownLatch(1));

        // when
        orderReceivedProducer.send(new ProducerRecord<>(kafkaTopics.getOrderReceived(),
                kafkaTopics.getOrderReceived(),
                getOrderReceived())).get();
        orderMessageDefaultConsumerAspect.getAfterProcessOrderReceivedEventLatch().await(30, TimeUnit.SECONDS);
        email_send actual = KafkaTestUtils.getSingleRecord(emailSendConsumer, kafkaTopics.getEmailSend()).value();

        // then
        assertEquals(0, orderMessageDefaultConsumerAspect.getAfterProcessOrderReceivedEventLatch().getCount());
        assertEquals(EmailService.CERTIFIED_COPY_ORDER_NOTIFICATION_API_APP_ID, actual.getAppId());
        assertNotNull(actual.getMessageId());
        assertEquals(EmailService.CERTIFIED_COPY_ORDER_NOTIFICATION_API_MESSAGE_TYPE,
                actual.getMessageType());
        assertEquals(EmailService.TOKEN_EMAIL_ADDRESS, actual.getEmailAddress());
        assertNotNull(actual.getData());
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
        actual.forEach(record -> {
            assertEquals("ORD-123123-123123", record.value().getReference());
            assertNotNull(record.value().getItem());
            assertEquals(getMidId(), record.value().getItem().getId());
        });
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

    private String getMidId() {
        return "MID-123123-" + midId++;
    }

    private static Stream<Arguments> certificateTestParameters() {
        return Stream.of(
                Arguments.of("/fixtures/certified-certificate.json", "Order containing one certificate"),
                Arguments.of("/fixtures/multi-certified-certificate.json", "Order containing multiple certificates")
        );
    }
}