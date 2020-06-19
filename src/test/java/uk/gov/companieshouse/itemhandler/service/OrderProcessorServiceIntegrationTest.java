package uk.gov.companieshouse.itemhandler.service;

import org.apache.kafka.common.errors.TimeoutException;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.companieshouse.itemhandler.exception.RetryableEmailSendException;
import uk.gov.companieshouse.itemhandler.model.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;
import static org.mockito.Mockito.when;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;
import static uk.gov.companieshouse.itemhandler.model.CertificateType.INCORPORATION;
import static uk.gov.companieshouse.itemhandler.model.CollectionLocation.BELFAST;
import static uk.gov.companieshouse.itemhandler.model.DeliveryTimescale.STANDARD;
import static uk.gov.companieshouse.itemhandler.model.ProductType.CERTIFICATE;

/**
 * Partially integration tests the {@link OrderProcessorService} to examine the handling of real kafka producer
 * originated exceptions.
 */
@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
@EmbeddedKafka
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
@FixMethodOrder(NAME_ASCENDING)
@TestPropertySource(properties={"certificate.order.confirmation.recipient = nobody@nowhere.com"})
public class OrderProcessorServiceIntegrationTest {

    private static final String ORDER_RECEIVED_URI = "/orders/ORDER-12345";

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

    @Autowired
    private OrderProcessorService serviceUnderTest;

    @Autowired
    private EmbeddedKafkaBroker broker;

    @MockBean
    private OrdersApiClientService ordersApiService;

    @Test
    public void kafkaServerUnavailableWrappedAsRetryableException() throws Exception {

        // Given
        final OrderData order = createOrder();
        when(ordersApiService.getOrderData(ORDER_RECEIVED_URI)).thenReturn(order);

        // This provokes a Kafka TimeoutException
        broker.getKafkaServers().get(0).shutdown();

        // When and then
        assertThatExceptionOfType(RetryableEmailSendException.class).isThrownBy(() ->
                serviceUnderTest.processOrderReceived(ORDER_RECEIVED_URI))
                .withCauseInstanceOf(TimeoutException.class)
                .withMessage("Exception caught sending message to Kafka.");

    }

    @Test
    public void onceKafkaServerAvailableAbleToProduce()
            throws Exception {

        // Given
        final OrderData order = createOrder();
        when(ordersApiService.getOrderData(ORDER_RECEIVED_URI)).thenReturn(order);

        // When and then
        assertThatCode(() -> serviceUnderTest.processOrderReceived(ORDER_RECEIVED_URI)).doesNotThrowAnyException();

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
