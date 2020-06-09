package uk.gov.companieshouse.itemhandler.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import uk.gov.companieshouse.itemhandler.email.CertificateOrderConfirmation;
import uk.gov.companieshouse.itemhandler.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.companieshouse.itemhandler.mapper.OrderDataToCertificateOrderConfirmationMapperConstants.TIME_OF_PAYMENT_FORMATTER;

/**
 * Unit tests the {@link OrderDataToCertificateOrderConfirmationMapper} interface and its implementation.
 */
@ExtendWith(SpringExtension.class)
@SpringJUnitConfig(OrderDataToCertificateOrderConfirmationMapperTest.Config.class)
public class OrderDataToCertificateOrderConfirmationMapperTest {

    private static final String[] FULL_CERTIFICATE_INCLUDES = new String[]{
            "Statement of good standing",
            "Registered office address",
            "Directors",
            "Secretaries",
            "Company objects"
    };

    private static final String[] CERTIFICATE_INCLUDES_WITHOUT_DIRECTOR_AND_SECRETARY_DETAILS = new String[]{
            "Statement of good standing",
            "Registered office address",
            "Company objects"
    };

    private static final LocalTime AM = LocalTime.of(7, 30, 15);
    private static final LocalTime PM = LocalTime.of(15, 30, 15);
    private static final LocalDate DATE = LocalDate.of(2020, 6, 4);
    private static final LocalDateTime MORNING_DATE_TIME = LocalDateTime.of(DATE, AM);
    private static final LocalDateTime AFTERNOON_DATE_TIME = LocalDateTime.of(DATE, PM);

    private static final String EXPECTED_AM_DATE_TIME_RENDERING = "04 June 2020 at 07:30";
    private static final String EXPECTED_PM_DATE_TIME_RENDERING = "04 June 2020 at 15:30";

    @Configuration
    @ComponentScan(basePackageClasses = {OrderDataToCertificateOrderConfirmationMapperTest.class})
    static class Config {}

    @Autowired
    private OrderDataToCertificateOrderConfirmationMapper mapperUnderTest;

    /**
     * Implements {@link OrderDataToCertificateOrderConfirmationMapper} to facilitate the testing of its default
     * methods.
     */
    static class TestOrderDataToCertificateOrderConfirmationMapper implements
            OrderDataToCertificateOrderConfirmationMapper {
        @Override
        public CertificateOrderConfirmation orderToConfirmation(OrderData order) {
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
        final CertificateItemOptions options = new CertificateItemOptions();
        options.setDeliveryTimescale(DeliveryTimescale.STANDARD);
        options.setCertificateType(CertificateType.INCORPORATION_WITH_ALL_NAME_CHANGES);

        options.setIncludeGoodStandingInformation(true);
        options.setRegisteredOfficeAddressDetails(new RegisteredOfficeAddressDetails());
        final DirectorOrSecretaryDetails directors = new DirectorOrSecretaryDetails();
        directors.setIncludeBasicInformation(true);
        options.setDirectorDetails(directors);
        final DirectorOrSecretaryDetails secretaries = new DirectorOrSecretaryDetails();
        secretaries.setIncludeBasicInformation(true);
        options.setSecretaryDetails(secretaries);
        options.setIncludeCompanyObjectsInformation(true);

        item.setItemOptions(options);
        order.setItems(singletonList(item));
        order.setOrderedAt(LocalDateTime.now());
        order.setTotalOrderCost("15");

        // When
        final CertificateOrderConfirmation confirmation = mapperUnderTest.orderToConfirmation(order);

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

        assertThat(confirmation.getDeliveryMethod(), is("Standard delivery"));
        assertThat(confirmation.getCompanyName(), is("THE COMPANY"));
        assertThat(confirmation.getCompanyNumber(), is("00000001"));
        assertThat(confirmation.getCertificateType(), is("Incorporation with all name changes"));
        assertThat(confirmation.getCertificateIncludes(), is(FULL_CERTIFICATE_INCLUDES));
        assertThat(confirmation.getTimeOfPayment(), is(TIME_OF_PAYMENT_FORMATTER.format(order.getOrderedAt())));
        assertThat(confirmation.getFeeAmount(), is("15"));
    }

    @Test
    void orderToConfirmationCopesWithMissingDirectorAndSecretaryDetails() {
        final OrderData order = new OrderData();
        final Item item = new Item();
        final CertificateItemOptions options = new CertificateItemOptions();
        options.setDeliveryTimescale(DeliveryTimescale.STANDARD);
        options.setCertificateType(CertificateType.INCORPORATION_WITH_ALL_NAME_CHANGES);

        options.setIncludeGoodStandingInformation(true);
        options.setRegisteredOfficeAddressDetails(new RegisteredOfficeAddressDetails());
        final DirectorOrSecretaryDetails directors = new DirectorOrSecretaryDetails();
        options.setDirectorDetails(directors);
        options.setIncludeCompanyObjectsInformation(true);

        item.setItemOptions(options);
        order.setItems(singletonList(item));
        order.setOrderedAt(LocalDateTime.now());

        final CertificateOrderConfirmation confirmation = mapperUnderTest.orderToConfirmation(order);
        assertThat(confirmation.getCertificateIncludes(), is(CERTIFICATE_INCLUDES_WITHOUT_DIRECTOR_AND_SECRETARY_DETAILS));
    }

    @Test
    void toSentenceCaseBehavesAsExpected() {
        final TestOrderDataToCertificateOrderConfirmationMapper mapperUnderTest =
                new TestOrderDataToCertificateOrderConfirmationMapper();
        assertThat(mapperUnderTest.toSentenceCase("INCORPORATION_WITH_ALL_NAME_CHANGES"), is("Incorporation with all name changes"));
        assertThat(mapperUnderTest.toSentenceCase("STANDARD delivery"), is("Standard delivery"));
        assertThat(mapperUnderTest.toSentenceCase("SAME_DAY delivery"), is("Same day delivery"));
    }

    @Test
    void getTimeOfPaymentBehavesAsExpected() {
        assertThat(mapperUnderTest.getTimeOfPayment(MORNING_DATE_TIME), is(EXPECTED_AM_DATE_TIME_RENDERING));
        assertThat(mapperUnderTest.getTimeOfPayment(AFTERNOON_DATE_TIME), is(EXPECTED_PM_DATE_TIME_RENDERING));
    }

}
