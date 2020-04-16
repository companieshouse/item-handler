package uk.gov.companieshouse.itemhandler.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import uk.gov.companieshouse.api.model.order.ActionedByApi;
import uk.gov.companieshouse.api.model.order.DeliveryDetailsApi;
import uk.gov.companieshouse.api.model.order.ItemApi;
import uk.gov.companieshouse.api.model.order.OrdersApi;
import uk.gov.companieshouse.api.model.order.item.*;
import uk.gov.companieshouse.itemhandler.model.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.companieshouse.api.model.order.item.CertificateTypeApi.INCORPORATION;
import static uk.gov.companieshouse.api.model.order.item.CollectionLocationApi.BELFAST;
import static uk.gov.companieshouse.api.model.order.item.DeliveryMethodApi.POSTAL;
import static uk.gov.companieshouse.api.model.order.item.DeliveryTimescaleApi.STANDARD;
import static uk.gov.companieshouse.api.model.order.item.IncludeAddressRecordsTypeApi.CURRENT;
import static uk.gov.companieshouse.api.model.order.item.IncludeDobTypeApi.PARTIAL;
import static uk.gov.companieshouse.api.model.order.item.ProductTypeApi.CERTIFICATE;

@ExtendWith(SpringExtension.class)
@SpringJUnitConfig(OrdersApiToOrderDataMapperTest.Config.class)
public class OrdersApiToOrderDataMapperTest {

    @Configuration
    @ComponentScan(basePackageClasses = {OrdersApiToOrderDataMapperTest.class})
    static class Config {}

    @Autowired
    private OrdersApiToOrderDataMapper mapper;

    private static final String ORDER_ETAG          = "etag-xyz";
    private static final String ORDER_KIND          = "order";
    private static final String ORDER_REF           = "reference-xyz";
    private static final String PAYMENT_REF         = "payment-ref-xyz";
    private static final String ORDER_TOTAL_COST    = "10";
    private static final String LINK_SELF           = "/link.self";
    private static final String ACTIONED_BY_EMAIL   = "demo@ch.gov.uk";
    private static final String ACTIONED_BY_ID      = "id-xyz";

    private static final String CONTACT_NUMBER      = "00006444";
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

    private static final boolean INCLUDE_ADDRESS = true;
    private static final boolean INCLUDE_APPOINTMENT_DATE = false;
    private static final boolean INCLUDE_BASIC_INFORMATION = true;
    private static final boolean INCLUDE_COUNTRY_OF_RESIDENCE = false;
    private static final IncludeDobTypeApi INCLUDE_DOB_TYPE = PARTIAL;
    private static final boolean INCLUDE_NATIONALITY= false;
    private static final boolean INCLUDE_OCCUPATION = true;
    private static final boolean INCLUDE_COMPANY_OBJECTS_INFORMATION = true;
    private static final boolean INCLUDE_EMAIL_COPY = true;
    private static final boolean INCLUDE_GOOD_STANDING_INFORMATION = false;

    private static final IncludeAddressRecordsTypeApi INCLUDE_ADDRESS_RECORDS_TYPE = CURRENT;
    private static final boolean INCLUDE_DATES = true;

    private final static CertificateItemOptionsApi ITEM_OPTIONS;
    private final static DirectorOrSecretaryDetailsApi DIRECTOR_OR_SECRETARY_DETAILS;
    private final static RegisteredOfficeAddressDetailsApi REGISTERED_OFFICE_ADDRESS_DETAILS;
    static {
        DIRECTOR_OR_SECRETARY_DETAILS = new DirectorOrSecretaryDetailsApi();
        DIRECTOR_OR_SECRETARY_DETAILS.setIncludeAddress(INCLUDE_ADDRESS);
        DIRECTOR_OR_SECRETARY_DETAILS.setIncludeAppointmentDate(INCLUDE_APPOINTMENT_DATE);
        DIRECTOR_OR_SECRETARY_DETAILS.setIncludeBasicInformation(INCLUDE_BASIC_INFORMATION);
        DIRECTOR_OR_SECRETARY_DETAILS.setIncludeCountryOfResidence(INCLUDE_COUNTRY_OF_RESIDENCE);
        DIRECTOR_OR_SECRETARY_DETAILS.setIncludeDobType(INCLUDE_DOB_TYPE);
        DIRECTOR_OR_SECRETARY_DETAILS.setIncludeNationality(INCLUDE_NATIONALITY);
        DIRECTOR_OR_SECRETARY_DETAILS.setIncludeOccupation(INCLUDE_OCCUPATION);

        REGISTERED_OFFICE_ADDRESS_DETAILS = new RegisteredOfficeAddressDetailsApi();
        REGISTERED_OFFICE_ADDRESS_DETAILS.setIncludeAddressRecordsType(INCLUDE_ADDRESS_RECORDS_TYPE);
        REGISTERED_OFFICE_ADDRESS_DETAILS.setIncludeDates(INCLUDE_DATES);

        ITEM_OPTIONS = new CertificateItemOptionsApi();
        ITEM_OPTIONS.setCertificateType(INCORPORATION);
        ITEM_OPTIONS.setCollectionLocation(BELFAST);
        ITEM_OPTIONS.setContactNumber(CONTACT_NUMBER);
        ITEM_OPTIONS.setDeliveryMethod(POSTAL);
        ITEM_OPTIONS.setDeliveryTimescale(STANDARD);
        ITEM_OPTIONS.setDirectorDetails(DIRECTOR_OR_SECRETARY_DETAILS);
        ITEM_OPTIONS.setIncludeCompanyObjectsInformation(INCLUDE_COMPANY_OBJECTS_INFORMATION);
        ITEM_OPTIONS.setIncludeEmailCopy(INCLUDE_EMAIL_COPY);
        ITEM_OPTIONS.setIncludeGoodStandingInformation(INCLUDE_GOOD_STANDING_INFORMATION);
        ITEM_OPTIONS.setRegisteredOfficeAddressDetails(REGISTERED_OFFICE_ADDRESS_DETAILS);
        ITEM_OPTIONS.setSecretaryDetails(DIRECTOR_OR_SECRETARY_DETAILS);
    }

    @Test
    public void ordersApiToOrderDataMapperTest(){
        OrdersApi ordersApi = new OrdersApi();
        ordersApi.setEtag(ORDER_ETAG);
        ordersApi.setKind(ORDER_KIND);
        ordersApi.setReference(ORDER_REF);
        ordersApi.setPaymentReference(PAYMENT_REF);
        ordersApi.setTotalOrderCost(ORDER_TOTAL_COST);
        LinksApi linksApi = new LinksApi();
        linksApi.setSelf(LINK_SELF);
        ordersApi.setLinks(linksApi);
        LocalDateTime orderedAt = LocalDateTime.now();
        ordersApi.setOrderedAt(orderedAt);
        ActionedByApi actionedByApi = new ActionedByApi();
        actionedByApi.setId(ACTIONED_BY_ID);
        actionedByApi.setEmail(ACTIONED_BY_EMAIL);
        ordersApi.setOrderedBy(actionedByApi);
        List<ItemApi> items = singletonList(setupTestItemApi());
        ordersApi.setItems(items);

        final OrderData actualOrderData = mapper.ordersApiToOrderData(ordersApi);
        assertThat(actualOrderData.getEtag(), is(ordersApi.getEtag()));
        assertThat(actualOrderData.getKind(), is(ordersApi.getKind()));
        assertThat(actualOrderData.getPaymentReference(), is(ordersApi.getPaymentReference()));
        assertThat(actualOrderData.getReference(), is(ordersApi.getReference()));
        assertThat(actualOrderData.getTotalOrderCost(), is(ordersApi.getTotalOrderCost()));
        assertThat(actualOrderData.getOrderedAt(), is(ordersApi.getOrderedAt()));

        Item item = actualOrderData.getItems().get(0);
        ItemApi itemApi = ordersApi.getItems().get(0);
        assertItemCosts(item.getItemCosts().get(0), itemApi.getItemCosts().get(0));
        assertOrderedBy(actualOrderData.getOrderedBy(), ordersApi.getOrderedBy());
        assertLinks(actualOrderData.getLinks(), ordersApi.getLinks());
        assertDeliveryDetails(actualOrderData.getDeliveryDetails(), ordersApi.getDeliveryDetails());
    }

    private void assertItemCosts(final ItemCosts costs, final ItemCostsApi costsApi) {
        assertThat(costs.getCalculatedCost(), is(costsApi.getCalculatedCost()));
    }

    private void assertOrderedBy(final ActionedBy orderedBy, final ActionedByApi orderedByApi){
        assertThat(orderedBy.getEmail(), is(orderedByApi.getEmail()));
        assertThat(orderedBy.getId(), is(orderedByApi.getId()));
    }

    private void assertLinks(final OrderLinks links, final LinksApi linksApi) {
        assertThat(links.getSelf(), is(linksApi.getSelf()));
    }

    private void assertDeliveryDetails(final DeliveryDetails deliveryDetails,
                                       final DeliveryDetailsApi deliveryDetailsApi) {
        assertThat(deliveryDetails.getForename(), is(deliveryDetailsApi.getForename()));
        assertThat(deliveryDetails.getSurname(), is(deliveryDetailsApi.getSurname()));
        assertThat(deliveryDetails.getAddressLine1(), is(deliveryDetailsApi.getAddressLine1()));
        assertThat(deliveryDetails.getAddressLine2(), is(deliveryDetailsApi.getAddressLine2()));
        assertThat(deliveryDetails.getPremises(), is(deliveryDetailsApi.getPremises()));
        assertThat(deliveryDetails.getLocality(), is(deliveryDetailsApi.getLocality()));
        assertThat(deliveryDetails.getRegion(), is(deliveryDetailsApi.getRegion()));
        assertThat(deliveryDetails.getPoBox(), is(deliveryDetailsApi.getPoBox()));
        assertThat(deliveryDetails.getPostalCode(), is(deliveryDetailsApi.getPostalCode()));
        assertThat(deliveryDetails.getCountry(), is(deliveryDetailsApi.getCountry()));
    }

    private ItemApi setupTestItemApi(){
        ItemApi item = new ItemApi();
        item.setCompanyName(COMPANY_NAME);
        item.setCompanyNumber(COMPANY_NUMBER);
        item.setCustomerReference(CUSTOMER_REFERENCE);
        item.setQuantity(QUANTITY);
        item.setDescription(DESCRIPTION);
        item.setDescriptionIdentifier(DESCRIPTION_IDENTIFIER);
        item.setDescriptionValues(DESCRIPTION_VALUES);
        ItemCostsApi itemCosts = new ItemCostsApi();
        itemCosts.setProductType(CERTIFICATE);
        itemCosts.setCalculatedCost("5");
        itemCosts.setItemCost("5");
        itemCosts.setDiscountApplied("0");
        item.setItemCosts(singletonList(itemCosts));
        item.setPostageCost(POSTAGE_COST);
        item.setTotalItemCost(TOTAL_ITEM_COST);
        item.setKind(ITEM_KIND);
        item.setPostalDelivery(POSTAL_DELIVERY);
        item.setItemOptions(ITEM_OPTIONS);
        item.setEtag(TOKEN_ETAG);

        return item;
    }
}
