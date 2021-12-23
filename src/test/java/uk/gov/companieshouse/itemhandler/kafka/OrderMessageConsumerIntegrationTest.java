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
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.test.annotation.DirtiesContext;
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
@DirtiesContext
@Import(EmbeddedKafkaBrokerConfiguration.class)
@TestPropertySource(locations = "classpath:application.properties")
class OrderMessageConsumerIntegrationTest {

    public static final String ORDER_REFERENCE_NUMBER = "87654321";
    public static final String ORDER_NOTIFICATION_REFERENCE = "/orders/" + ORDER_REFERENCE_NUMBER;
    private static MockServerContainer container;
    private MockServerClient client;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaConsumer<String, email_send> emailSendConsumer;

    @Autowired
    private KafkaConsumer<String, ChdItemOrdered> chsItemOrderedConsumer;

    @Autowired
    private KafkaProducer<String, OrderReceived> orderReceivedProducer;

    @Autowired
    private KafkaProducer<String, BadOrderReceived> badOrderReceivedKafkaProducer;

    @Autowired
    private KafkaTopics kafkaTopics;

    @Autowired
    private OrderMessageConsumer orderMessageConsumer;

    @Autowired
    private KafkaConsumer<String, OrderReceived> orderReceivedConsumer;

    @SpyBean
    private OrderProcessResponseHandler orderProcessResponseHandler;

    @SpyBean
    private Logger logger;

    @Captor
    private ArgumentCaptor<String> argumentCaptor;

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

    private static OrderReceived getOrderReceived() {
        OrderReceived orderReceived = new OrderReceived();
        orderReceived.setOrderUri(ORDER_NOTIFICATION_REFERENCE);
        return orderReceived;
    }

    private static BadOrderReceived getBadOrderReceived() {
        BadOrderReceived orderReceived = new BadOrderReceived();
        orderReceived.setOrderUri2(ORDER_NOTIFICATION_REFERENCE);
        return orderReceived;
    }

    @BeforeEach
    void setup() {
        client = new MockServerClient(container.getHost(), container.getServerPort());
    }

    @AfterEach
    void teardown() {
        client.reset();
    }

    @Test
    void testConsumesCertificateOrderReceivedFromEmailSendTopic() throws ExecutionException, InterruptedException, IOException {
        //given
        client.when(request()
                        .withPath(ORDER_NOTIFICATION_REFERENCE)
                        .withMethod(HttpMethod.GET.toString()))
                .respond(response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withBody(JsonBody.json(IOUtils.resourceToString(
                                "/fixtures/certified-certificate.json",
                                StandardCharsets.UTF_8))));
        orderMessageConsumer.setPostOrderReceivedEventLatch(new CountDownLatch(1));

        // when
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(emailSendConsumer, kafkaTopics.getEmailSend());
        orderReceivedProducer.send(new ProducerRecord<>(
                kafkaTopics.getOrderReceived(),
                kafkaTopics.getOrderReceived(),
                getOrderReceived())).get();
        orderMessageConsumer.getPostOrderReceivedEventLatch().await(30, TimeUnit.SECONDS);
        email_send actual = emailSendConsumer.poll(Duration.ofSeconds(15))
                .iterator()
                .next()
                .value();

        // then
        assertEquals(EmailService.CERTIFICATE_ORDER_NOTIFICATION_API_APP_ID, actual.getAppId());
        assertNotNull(actual.getMessageId());
        assertEquals(EmailService.CERTIFICATE_ORDER_NOTIFICATION_API_MESSAGE_TYPE,
                actual.getMessageType());
        assertEquals(EmailService.TOKEN_EMAIL_ADDRESS, actual.getEmailAddress());
        assertNotNull(actual.getData());
    }

    @Test
    void testConsumesCertifiedDocumentOrderReceivedFromEmailSendTopic() throws ExecutionException, InterruptedException, IOException {
        // given
        client.when(request()
                        .withPath(ORDER_NOTIFICATION_REFERENCE)
                        .withMethod(HttpMethod.GET.toString()))
                .respond(response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, "application/json")
                        .withBody(JsonBody.json(IOUtils.resourceToString(
                                "/fixtures/certified-copy.json",
                                StandardCharsets.UTF_8))));
        orderMessageConsumer.setPostOrderReceivedEventLatch(new CountDownLatch(1));

        // when
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(emailSendConsumer, kafkaTopics.getEmailSend());
        orderReceivedProducer.send(new ProducerRecord<>(kafkaTopics.getOrderReceived(),
                kafkaTopics.getOrderReceived(),
                getOrderReceived())).get();
        orderMessageConsumer.getPostOrderReceivedEventLatch().await(30, TimeUnit.SECONDS);
        email_send actual = emailSendConsumer.poll(Duration.ofSeconds(15))
                .iterator()
                .next()
                .value();

        // then
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
                        .withPath(ORDER_NOTIFICATION_REFERENCE)
                        .withMethod(HttpMethod.GET.toString()))
                .respond(response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, "application/json")
                        .withBody(JsonBody.json(IOUtils.resourceToString(
                                "/fixtures/missing-image-delivery.json",
                                StandardCharsets.UTF_8))));
        orderMessageConsumer.setPostOrderReceivedEventLatch(new CountDownLatch(1));

        // when
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(chsItemOrderedConsumer, kafkaTopics.getChdItemOrdered());
        orderReceivedProducer.send(new ProducerRecord<>(kafkaTopics.getOrderReceived(),
                kafkaTopics.getOrderReceived(),
                getOrderReceived())).get();
        orderMessageConsumer.getPostOrderReceivedEventLatch().await(30, TimeUnit.SECONDS);
        ChdItemOrdered actual = chsItemOrderedConsumer.poll(Duration.ofSeconds(15))
                .iterator()
                .next()
                .value();

        // then
        assertEquals("ORD-123123-123123", actual.getReference());
        assertNotNull(actual.getItem());
    }

    @Test
    void testConsumesCertificateOrderReceivedFromRetryTopic() throws ExecutionException, InterruptedException, IOException {
        //given
        client.when(request()
                        .withPath(ORDER_NOTIFICATION_REFERENCE)
                        .withMethod(HttpMethod.GET.toString()))
                .respond(response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withBody(JsonBody.json(IOUtils.resourceToString(
                                "/fixtures/certified-certificate.json",
                                StandardCharsets.UTF_8))));
        orderMessageConsumer.setPostRetryEventLatch(new CountDownLatch(1));


        // when
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(emailSendConsumer, kafkaTopics.getEmailSend());
        orderReceivedProducer.send(new ProducerRecord<>(
                kafkaTopics.getOrderReceivedNotificationRetry(),
                kafkaTopics.getOrderReceivedNotificationRetry(),
                getOrderReceived())).get();
        orderMessageConsumer.getPostRetryEventLatch().await(30, TimeUnit.SECONDS);
        email_send actual = emailSendConsumer.poll(Duration.ofSeconds(15))
                .iterator()
                .next()
                .value();

        // then
        assertEquals(EmailService.CERTIFICATE_ORDER_NOTIFICATION_API_APP_ID, actual.getAppId());
        assertNotNull(actual.getMessageId());
        assertEquals(EmailService.CERTIFICATE_ORDER_NOTIFICATION_API_MESSAGE_TYPE,
                actual.getMessageType());
        assertEquals(EmailService.TOKEN_EMAIL_ADDRESS, actual.getEmailAddress());
        assertNotNull(actual.getData());
    }

    @Test
    void testConsumesCertifiedDocumentOrderReceivedFromRetryTopic() throws ExecutionException, InterruptedException, IOException {
        //given
        client.when(request()
                        .withPath(ORDER_NOTIFICATION_REFERENCE)
                        .withMethod(HttpMethod.GET.toString()))
                .respond(response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withBody(JsonBody.json(IOUtils.resourceToString(
                                "/fixtures/certified-copy.json",
                                StandardCharsets.UTF_8))));
        orderMessageConsumer.setPostRetryEventLatch(new CountDownLatch(1));

        // when
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(emailSendConsumer, kafkaTopics.getEmailSend());
        orderReceivedProducer.send(new ProducerRecord<>(
                kafkaTopics.getOrderReceivedNotificationRetry(),
                kafkaTopics.getOrderReceivedNotificationRetry(),
                getOrderReceived())).get();
        orderMessageConsumer.getPostRetryEventLatch().await(30, TimeUnit.SECONDS);
        email_send actual = emailSendConsumer.poll(Duration.ofSeconds(15))
                .iterator()
                .next()
                .value();

        // then
        assertEquals(EmailService.CERTIFIED_COPY_ORDER_NOTIFICATION_API_APP_ID, actual.getAppId());
        assertNotNull(actual.getMessageId());
        assertEquals(EmailService.CERTIFIED_COPY_ORDER_NOTIFICATION_API_MESSAGE_TYPE,
                actual.getMessageType());
        assertEquals(EmailService.TOKEN_EMAIL_ADDRESS, actual.getEmailAddress());
        assertNotNull(actual.getData());
    }

    @Test
    void testConsumesMissingImageDeliveryOrderReceivedFromRetryTopic() throws ExecutionException, InterruptedException, IOException {
        // given
        client.when(request()
                .withPath(ORDER_NOTIFICATION_REFERENCE)
                .withMethod(HttpMethod.GET.toString()))
                .respond(response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, "application/json")
                        .withBody(JsonBody.json(IOUtils.resourceToString(
                                "/fixtures/missing-image-delivery.json",
                                StandardCharsets.UTF_8))));
        orderMessageConsumer.setPostRetryEventLatch(new CountDownLatch(1));

        // when
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(chsItemOrderedConsumer, kafkaTopics.getChdItemOrdered());
        orderReceivedProducer.send(new ProducerRecord<>(
                kafkaTopics.getOrderReceivedNotificationRetry(),
                kafkaTopics.getOrderReceivedNotificationRetry(),
                getOrderReceived())).get();
        orderMessageConsumer.getPostRetryEventLatch().await(30, TimeUnit.SECONDS);
        ChdItemOrdered actual = chsItemOrderedConsumer.poll(Duration.ofSeconds(15))
                .iterator()
                .next()
                .value();

        // then
        assertEquals("ORD-123123-123123", actual.getReference());
        assertNotNull(actual.getItem());
    }

    @Test
    void testPublishesOrderReceivedToRetryTopicWhenOrdersApiIsUnavailable() throws ExecutionException, InterruptedException, IOException {
        //given
        client.when(request()
                .withPath(ORDER_NOTIFICATION_REFERENCE)
                .withMethod(HttpMethod.GET.toString()))
                .respond(response()
                        .withStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()));

        orderMessageConsumer.setPreRetryEventLatch(new CountDownLatch(1));
        orderMessageConsumer.setPostOrderReceivedEventLatch(new CountDownLatch(1));
        orderMessageConsumer.setPostRetryEventLatch(new CountDownLatch(1));

        orderReceivedProducer.send(new ProducerRecord<>(
                kafkaTopics.getOrderReceived(),
                kafkaTopics.getOrderReceived(),
                getOrderReceived())).get();

        // sync so that retry consumer does not consume early
        orderMessageConsumer.getPostOrderReceivedEventLatch().await(30, TimeUnit.SECONDS);
        client.reset();

        client.when(request()
                .withPath(ORDER_NOTIFICATION_REFERENCE)
                .withMethod(HttpMethod.GET.toString()))
                .respond(response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withBody(JsonBody.json(IOUtils.resourceToString(
                                "/fixtures/certified-certificate.json",
                                StandardCharsets.UTF_8))));

        orderMessageConsumer.getPreRetryEventLatch().countDown();

        // when
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(emailSendConsumer, kafkaTopics.getEmailSend());

        // sync so that retry consumer has completed and app has published email onto email send
        // topic
        orderMessageConsumer.getPostRetryEventLatch().await(30, TimeUnit.SECONDS);
        email_send actual = emailSendConsumer.poll(Duration.ofSeconds(15))
                .iterator()
                .next()
                .value();

        // then
        assertEquals(EmailService.CERTIFICATE_ORDER_NOTIFICATION_API_APP_ID, actual.getAppId());
        assertNotNull(actual.getMessageId());
        assertEquals(EmailService.CERTIFICATE_ORDER_NOTIFICATION_API_MESSAGE_TYPE,
                actual.getMessageType());
        assertEquals(EmailService.TOKEN_EMAIL_ADDRESS, actual.getEmailAddress());
        assertNotNull(actual.getData());
    }

    @Test
    void testPublishesOrderReceivedToErrorTopicAfterMaxRetryAttempts() throws ExecutionException, InterruptedException {
        //given
        client.when(request()
                .withPath(ORDER_NOTIFICATION_REFERENCE)
                .withMethod(HttpMethod.GET.toString()))
                .respond(response()
                        .withStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()));

        orderProcessResponseHandler.setPostPublishToErrorTopicLatch(new CountDownLatch(1));

        orderReceivedProducer.send(new ProducerRecord<>(
                kafkaTopics.getOrderReceived(),
                kafkaTopics.getOrderReceived(),
                getOrderReceived())).get();

        // when
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(orderReceivedConsumer,
                kafkaTopics.getOrderReceivedNotificationError());

        // sync so that retry consumer has completed and app has published email onto email send
        // topic
        orderProcessResponseHandler.getPostPublishToErrorTopicLatch().await(30, TimeUnit.SECONDS);
        OrderReceived actual = orderReceivedConsumer.poll(Duration.ofSeconds(15))
                .iterator()
                .next()
                .value();

        // then
        assertEquals(ORDER_NOTIFICATION_REFERENCE, actual.getOrderUri());
        assertEquals(0, actual.getAttempt());
    }

    @Test
    void testLogAnErrorWhenOrdersApiReturnsOrderNotFound() throws ExecutionException, InterruptedException, IOException {
        //given
        client.when(request()
                .withPath(ORDER_NOTIFICATION_REFERENCE)
                .withMethod(HttpMethod.GET.toString()))
                .respond(response()
                        .withStatusCode(HttpStatus.NOT_FOUND.value()));
        orderMessageConsumer.setPostOrderReceivedEventLatch(new CountDownLatch(1));

        // when
        orderReceivedProducer.send(new ProducerRecord<>(
                kafkaTopics.getOrderReceived(),
                kafkaTopics.getOrderReceived(),
                getOrderReceived())).get();
        orderMessageConsumer.getPostOrderReceivedEventLatch().await(30, TimeUnit.SECONDS);

        // then
        verify(orderProcessResponseHandler).serviceError(any());
        verify(logger).error(argumentCaptor.capture(), anyMap());
        assertEquals("order-received message processing failed with a "
                + "non-recoverable exception", argumentCaptor.getValue());
    }

    @Test
    void testLogAnErrorWhenOrderReceivedCannotBeDeserialised() throws ExecutionException, InterruptedException, IOException {
        //given
        client.when(request()
                        .withPath(ORDER_NOTIFICATION_REFERENCE)
                        .withMethod(HttpMethod.GET.toString()))
                .respond(response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withBody(JsonBody.json(IOUtils.resourceToString(
                                "/fixtures/certified-certificate.json",
                                StandardCharsets.UTF_8))));
        orderMessageConsumer.setPostOrderReceivedEventLatch(new CountDownLatch(1));

        // when
        badOrderReceivedKafkaProducer.send(new ProducerRecord<>(
                kafkaTopics.getOrderReceived(),
                kafkaTopics.getOrderReceived(),
                getBadOrderReceived())).get();
        orderMessageConsumer.getPostOrderReceivedEventLatch().await(30, TimeUnit.SECONDS);

        // then
        verify(orderProcessResponseHandler).serviceError(any());
        verify(logger).error(argumentCaptor.capture(), anyMap());
        assertEquals("order-received message processing failed with a "
                + "non-recoverable exception", argumentCaptor.getValue());
    }
}