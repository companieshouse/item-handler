package uk.gov.companieshouse.itemhandler.kafka;

import org.aspectj.lang.annotation.Aspect;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.companieshouse.itemhandler.model.*;
import uk.gov.companieshouse.itemhandler.service.OrdersApiClientService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.*;
import static uk.gov.companieshouse.itemhandler.model.CertificateType.INCORPORATION;
import static uk.gov.companieshouse.itemhandler.model.CollectionLocation.BELFAST;
import static uk.gov.companieshouse.itemhandler.model.DeliveryTimescale.STANDARD;
import static uk.gov.companieshouse.itemhandler.model.ProductType.CERTIFICATE;

// TODO GCI-1181 Is this test worth completing?
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
@Aspect
public class OrdersKafkaConsumerRetryIntegrationTest {
    private static final String ORDER_RECEIVED_TOPIC = "order-received";

    private static final String ORDER_RECEIVED_URI = "/orders/ORDER-12345";
    private static final String ORDER_RECEIVED_MESSAGE_JSON = "{\"order_uri\": \"/orders/ORDER-12345\"}";

    private static final String ORDER_ETAG          = "etag-xyz";
    private static final String ORDER_KIND          = "order";
    private static final String ORDER_REF           = "reference-xyz";
    private static final String PAYMENT_REF         = "payment-ref-xyz";
    private static final String ORDER_TOTAL_COST    = "10";
    private static final String LINK_SELF           = "/link.self";
    private static final String ACTIONED_BY_EMAIL   = "demo@ch.gov.uk";
    private static final String ACTIONED_BY_ID      = "id-xyz";

    private static final String COMPANY_NUMBER      = "00006444";
    private static final String COMPANY_NAME        = "Phillips & Daughters";
    private static final int QUANTITY               = 10;
    private static final String DESCRIPTION         = "Certificate";
    private static final String DESCRIPTION_IDENTIFIER = "Description Identifier";
    private static final Map<String, String> DESCRIPTION_VALUES = singletonMap("key1", "value1");
    private static final String POSTAGE_COST        = "0";
    private static final String TOTAL_ITEM_COST     = "100";
    private static final String ITEM_KIND           = "certificate";
    private static final boolean POSTAL_DELIVERY    = true;
    private static final String CUSTOMER_REFERENCE  = "Certificate ordered by NJ.";
    private static final String TOKEN_ETAG          = "9d39ea69b64c80ca42ed72328b48c303c4445e28";

    @ClassRule
    public static final EnvironmentVariables ENVIRONMENT_VARIABLES = new EnvironmentVariables();

    @Autowired
    private OrdersKafkaProducer kafkaProducer;

    @Autowired
    private OrdersKafkaConsumerWrapper consumerWrapper;

    @MockBean
    private OrdersApiClientService ordersApiService;

    @Service
    @Aspect
    public static class EmailSendKafkaProducerWrapper {

        @Autowired
        private EmbeddedKafkaBroker broker;

        public EmailSendKafkaProducerWrapper() {

        }

        @org.aspectj.lang.annotation.Before(value =
                "execution(* uk.gov.companieshouse.itemhandler.kafka.EmailSendKafkaProducer.sendMessage(..)) " +
                        "&& args(message, orderReference)")
        public void beforeEmailSent(final Message message, final String orderReference) {
            // TODO GCI-1181 The idea here would be to stop the kafka server just before the email sending
            // is triggered.
            // This provokes a Kafka TimeoutException
            broker.getKafkaServers().get(0).shutdown();
        }

    }

    @After
    public void tearDown() {
        consumerWrapper.reset();
        reset();
    }

    @Test
    @Ignore // TODO GCI-1181 Is this test worth completing?
    public void kafkaServerUnavailableWrappedAsRetryableException() throws Exception {

        // Given
        final OrderData order = createOrder();
        when(ordersApiService.getOrderData(ORDER_RECEIVED_URI)).thenReturn(order);

        // When
        kafkaProducer.sendMessage(consumerWrapper.createMessage(ORDER_RECEIVED_URI, ORDER_RECEIVED_TOPIC));

        // Then
        // TODO GCI-1181 Is there a better way to get the mocking working across the threads?
        // See https://javadoc.io/static/org.mockito/mockito-core/3.3.3/org/mockito/Mockito.html#22.
        // This verify with timeout is currently necessary for the ordersApiService mocking to work correctly
        // between the main and item-handler-order-received-0-C-1 threads.
        verify(ordersApiService, timeout(2000)).getOrderData(ORDER_RECEIVED_URI);
    }

    private OrderData createOrder() {
        final OrderData order = new OrderData();
        order.setEtag(ORDER_ETAG);
        order.setKind(ORDER_KIND);
        order.setReference(ORDER_REF);
        order.setPaymentReference(PAYMENT_REF);
        order.setTotalOrderCost(ORDER_TOTAL_COST);
        final OrderLinks links = new OrderLinks();
        links.setSelf(LINK_SELF);
        order.setLinks(links);
        final LocalDateTime orderedAt = LocalDateTime.now();
        order.setOrderedAt(orderedAt);
        final ActionedBy actionedBy = new ActionedBy();
        actionedBy.setId(ACTIONED_BY_ID);
        actionedBy.setEmail(ACTIONED_BY_EMAIL);
        order.setOrderedBy(actionedBy);
        final List<Item> items = singletonList(createItem());
        order.setItems(items);
        return order;
    }

    private Item createItem(){
        final Item item = new Item();
        item.setCompanyName(COMPANY_NAME);
        item.setCompanyNumber(COMPANY_NUMBER);
        item.setCustomerReference(CUSTOMER_REFERENCE);
        item.setQuantity(QUANTITY);
        item.setDescription(DESCRIPTION);
        item.setDescriptionIdentifier(DESCRIPTION_IDENTIFIER);
        item.setDescriptionValues(DESCRIPTION_VALUES);
        final ItemCosts itemCosts = new ItemCosts();
        itemCosts.setProductType(CERTIFICATE);
        itemCosts.setCalculatedCost("5");
        itemCosts.setItemCost("5");
        itemCosts.setDiscountApplied("0");
        item.setItemCosts(singletonList(itemCosts));
        item.setPostageCost(POSTAGE_COST);
        item.setTotalItemCost(TOTAL_ITEM_COST);
        item.setKind(ITEM_KIND);
        item.setPostalDelivery(POSTAL_DELIVERY);
        final CertificateItemOptions options = new CertificateItemOptions();
        options.setCertificateType(INCORPORATION);
        options.setCollectionLocation(BELFAST);
        options.setDeliveryTimescale(STANDARD);
        item.setItemOptions(options);
        item.setEtag(TOKEN_ETAG);

        return item;
    }


}
