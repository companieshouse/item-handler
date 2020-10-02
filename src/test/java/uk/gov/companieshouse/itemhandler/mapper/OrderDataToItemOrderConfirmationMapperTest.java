package uk.gov.companieshouse.itemhandler.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import uk.gov.companieshouse.itemhandler.email.ItemOrderConfirmation;
import uk.gov.companieshouse.itemhandler.model.ActionedBy;
import uk.gov.companieshouse.itemhandler.model.CertifiedCopyItemOptions;
import uk.gov.companieshouse.itemhandler.model.DeliveryDetails;
import uk.gov.companieshouse.itemhandler.model.DeliveryTimescale;
import uk.gov.companieshouse.itemhandler.model.FilingHistoryDocument;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.ItemCosts;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.itemhandler.service.FilingHistoryDescriptionProviderService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.itemhandler.util.DateConstants.DATETIME_OF_PAYMENT_FORMATTER;

/**
 * Unit tests the {@link OrderDataToCertificateOrderConfirmationMapper} interface and its implementation.
 */
@ExtendWith(SpringExtension.class)
@SpringJUnitConfig(OrderDataToItemOrderConfirmationMapperTest.Config.class)
class OrderDataToItemOrderConfirmationMapperTest {

    private static final LocalTime AM = LocalTime.of(7, 30, 15);
    private static final LocalTime PM = LocalTime.of(15, 30, 15);
    private static final LocalDate DATE = LocalDate.of(2020, 6, 4);
    private static final LocalDateTime MORNING_DATE_TIME = LocalDateTime.of(DATE, AM);
    private static final LocalDateTime AFTERNOON_DATE_TIME = LocalDateTime.of(DATE, PM);

    private static final String EXPECTED_AM_DATE_TIME_RENDERING = "04 June 2020 at 07:30";
    private static final String EXPECTED_PM_DATE_TIME_RENDERING = "04 June 2020 at 15:30";

    private static final String DATE_FILED = "2009-08-23";
    private static final String EXPECTED_REFORMATTED_DATE_FILED = "23 Aug 2009";

    @Configuration
    @ComponentScan(basePackageClasses = {OrderDataToItemOrderConfirmationMapperTest.class})
    static class Config {}

    @MockBean
    FilingHistoryDescriptionProviderService filingHistoryDescriptionProviderService;

    @Autowired
    private OrderDataToItemOrderConfirmationMapper mapperUnderTest;

    /**
     * Implements {@link OrderDataToItemOrderConfirmationMapper} to facilitate the testing of its default
     * methods.
     */
    static class TestOrderDataToItemOrderConfirmationMapper extends OrderDataToItemOrderConfirmationMapper {
        TestOrderDataToItemOrderConfirmationMapper() {
            super();
        }

        @Override
        public ItemOrderConfirmation orderToConfirmation(OrderData order) {
            return null; // Implemented only to satisfy requirement of an interface implementation
        }
    }

    @Test
    void orderToConfirmationBehavesAsExpected() {

        // Given
        final OrderData order = new OrderData();
        order.setReference("ORD-108815-904831");
        order.setPaymentReference("orderable_item_ORD-108815-904831");

        final ActionedBy orderedBy = new ActionedBy();
        orderedBy.setEmail("demo@ch.gov.uk");
        order.setOrderedBy(orderedBy);

        final DeliveryDetails delivery = new DeliveryDetails();
        delivery.setForename("Jenny");
        delivery.setSurname("Wilson");
        delivery.setAddressLine1("Kemp House Capital Office");
        delivery.setAddressLine2("LTD");
        delivery.setLocality("Kemp House");
        delivery.setPremises("152-160 City Road");
        delivery.setRegion("London");
        delivery.setPostalCode("EC1V 2NX");
        delivery.setCountry("England");

        order.setDeliveryDetails(delivery);
        final Item item = new Item();
        item.setCompanyName("THE COMPANY");
        item.setCompanyNumber("00000001");
        item.setKind("item#certified-copy");
        final CertifiedCopyItemOptions options = new CertifiedCopyItemOptions();
        options.setDeliveryTimescale(DeliveryTimescale.STANDARD);

        FilingHistoryDocument filingHistoryDocument = new FilingHistoryDocument();
        filingHistoryDocument.setFilingHistoryDate("2018-02-15");
        filingHistoryDocument.setFilingHistoryDescription("appoint-person-director-company-with-name-date");
        filingHistoryDocument.setFilingHistoryDescriptionValues(singletonMap("key", "value"));
        filingHistoryDocument.setFilingHistoryType("AP01");
        List<FilingHistoryDocument> filingHistoryDocuments = new ArrayList<>();
        filingHistoryDocuments.add(filingHistoryDocument);
        options.setFilingHistoryDocuments(filingHistoryDocuments);

        List<ItemCosts> itemCosts = new ArrayList<>();
        ItemCosts itemCost = new ItemCosts();
        itemCost.setItemCost("15");
        itemCost.setDiscountApplied("0");
        itemCost.setCalculatedCost("15");
        itemCosts.add(itemCost);

        item.setItemOptions(options);
        item.setItemCosts(itemCosts);
        order.setItems(singletonList(item));
        order.setOrderedAt(LocalDateTime.now());
        order.setTotalOrderCost("15");

        // When
        when(filingHistoryDescriptionProviderService.mapFilingHistoryDescription(anyString(), anyMap()))
                .thenReturn("Appointment of Ms Sharon Michelle White as a Director on 01 Feb 2018");
        final ItemOrderConfirmation confirmation = mapperUnderTest.orderToConfirmation(order);

        // Then
        assertThat(confirmation.getTo(), is(nullValue()));

        assertThat(confirmation.getOrderReferenceNumber(), is("ORD-108815-904831"));
        assertThat(confirmation.getPaymentReference(), is("orderable_item_ORD-108815-904831"));

        assertThat(confirmation.getEmailAddress(), is("demo@ch.gov.uk"));

        assertThat(confirmation.getForename(), is("Jenny"));
        assertThat(confirmation.getSurname(), is("Wilson"));
        assertThat(confirmation.getAddressLine1(), is("Kemp House Capital Office"));
        assertThat(confirmation.getAddressLine2(), is("LTD"));
        assertThat(confirmation.getHouseName(), is("Kemp House"));
        assertThat(confirmation.getHouseNumberStreetName(), is("152-160 City Road"));
        assertThat(confirmation.getCity(), is("London"));
        assertThat(confirmation.getPostCode(), is("EC1V 2NX"));
        assertThat(confirmation.getCountry(), is("England"));

        assertThat(confirmation.getDeliveryMethod(), is("Standard delivery (aim to dispatch within 4 working days)"));
        assertThat(confirmation.getCompanyName(), is("THE COMPANY"));
        assertThat(confirmation.getCompanyNumber(), is("00000001"));
        assertThat(confirmation.getTimeOfPayment(), is(DATETIME_OF_PAYMENT_FORMATTER.format(order.getOrderedAt())));
        assertThat(confirmation.getTotalFee(), is("15"));

        assertThat(confirmation.getItemDetails().get(0).getDateFiled(), is("15 Feb 2018"));
        assertThat(confirmation.getItemDetails().get(0).getType(), is("AP01"));
        assertThat(confirmation.getItemDetails().get(0).getDescription(),
                is("Appointment of Ms Sharon Michelle White as a Director on 01 Feb 2018"));
        assertThat(confirmation.getItemDetails().get(0).getFee(), is("15"));
    }

    @Test
    void toSentenceCaseBehavesAsExpected() {
        final TestOrderDataToItemOrderConfirmationMapper mapperUnderTest =
                new TestOrderDataToItemOrderConfirmationMapper();
        assertThat(mapperUnderTest.toSentenceCase("INCORPORATION_WITH_ALL_NAME_CHANGES"), is("Incorporation with all name changes"));
        assertThat(mapperUnderTest.toSentenceCase("STANDARD delivery"), is("Standard delivery"));
        assertThat(mapperUnderTest.toSentenceCase("SAME_DAY delivery"), is("Same day delivery"));
    }

    @Test
    void getTimeOfPaymentBehavesAsExpected() {
        assertThat(mapperUnderTest.getTimeOfPayment(MORNING_DATE_TIME), is(EXPECTED_AM_DATE_TIME_RENDERING));
        assertThat(mapperUnderTest.getTimeOfPayment(AFTERNOON_DATE_TIME), is(EXPECTED_PM_DATE_TIME_RENDERING));
    }

    @Test
    void reformatDateFiledBehavesAsExpected() {
        assertThat(mapperUnderTest.reformatDateFiled(DATE_FILED), is(EXPECTED_REFORMATTED_DATE_FILED));
    }

}
