package uk.gov.companieshouse.itemhandler.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.mockserver.client.MockServerClient;
import org.mockserver.model.JsonBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import uk.gov.companieshouse.orders.OrderReceived;

@SpringBootTest
@DirtiesContext
@Import(EmbeddedKafkaBrokerConfiguration.class)
@TestPropertySource(locations = "classpath:application.properties",
        properties = {"uk.gov.companieshouse.item-handler.error-consumer=true"})
public class OrderMessageConsumerIntegrationErrorModeTest {

    public static final String ORDER_REFERENCE_NUMBER = "87654321";
    public static final String ORDER_NOTIFICATION_REFERENCE = "/orders/" + ORDER_REFERENCE_NUMBER;
    private static MockServerContainer container;
    private MockServerClient client;
    private CountDownLatch startupLatch;
    private CountDownLatch eventLatch;
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaConsumer<String, email_send> emailSendConsumer;

    @Autowired
    private KafkaProducer<String, OrderReceived> orderReceivedProducer;

    @Autowired
    private KafkaTopics kafkaTopics;

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

    @BeforeEach
    void setup() {
        client = new MockServerClient(container.getHost(), container.getServerPort());
        startupLatch = new CountDownLatch(1);
        OrderMessageConsumer.setStartupLatch(startupLatch);
        eventLatch = new CountDownLatch(1);
        OrderMessageConsumer.setEventLatch(eventLatch);
    }

    @AfterEach
    void teardown() {
        client.reset();
    }

    @Test
    void testConsumesCertificateOrderReceivedFromErrorTopic() throws
            ExecutionException, InterruptedException,
            IOException {
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

        // when
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(emailSendConsumer,
                kafkaTopics.getEmailSend());
        ProducerRecord<String, OrderReceived> producerRecord = new ProducerRecord<>(
                kafkaTopics.getOrderReceivedNotificationError(),
                kafkaTopics.getOrderReceivedNotificationError(),
                getOrderReceived());
        orderReceivedProducer.send(producerRecord).get();
        startupLatch.countDown();
        eventLatch.await(30, TimeUnit.SECONDS);
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
}