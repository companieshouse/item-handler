package uk.gov.companieshouse.itemhandler.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import uk.gov.companieshouse.itemhandler.email.CertificateOrderConfirmation;
import uk.gov.companieshouse.itemhandler.model.*;
import uk.gov.companieshouse.itemhandler.service.FilingHistoryDescriptionProviderService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.companieshouse.itemhandler.model.IncludeAddressRecordsType.*;
import static uk.gov.companieshouse.itemhandler.util.DateConstants.DATETIME_OF_PAYMENT_FORMATTER;

/**
 * Unit tests the {@link OrderDataToCertificateOrderConfirmationMapper} interface and its implementation.
 */
@ExtendWith(SpringExtension.class)
@SpringJUnitConfig(OrderDataToCertificateOrderConfirmationMapperTest.Config.class)
public class OrderDataToCertificateOrderConfirmationMapperTest {

    private static final String[] FULL_CERTIFICATE_INCLUDES = new String[]{
            "Statement of good standing",
            "Secretaries",
            "Company objects"
    };

    private static final String[] CERTIFICATE_INCLUDES_WITHOUT_DIRECTOR_AND_SECRETARY_DETAILS = new String[]{
            "Statement of good standing",
            "Registered office address",
            "Company objects"
    };

    private OrderData order;
    private ActionedBy orderedBy;
    private DeliveryDetails delivery;
    private Item item;
    private CertificateItemOptions options;

    private static final LocalTime AM = LocalTime.of(7, 30, 15);
    private static final LocalTime PM = LocalTime.of(15, 30, 15);
    private static final LocalDate DATE = LocalDate.of(2020, 6, 4);
    private static final LocalDateTime MORNING_DATE_TIME = LocalDateTime.of(DATE, AM);
    private static final LocalDateTime AFTERNOON_DATE_TIME = LocalDateTime.of(DATE, PM);

    private static final String EXPECTED_AM_DATE_TIME_RENDERING = "04 June 2020 at 07:30";
    private static final String EXPECTED_PM_DATE_TIME_RENDERING = "04 June 2020 at 15:30";

    @Configuration
    @ComponentScan(basePackageClasses = {OrderDataToCertificateOrderConfirmationMapperTest.class})
    static class Config {
        @Bean
        public FilingHistoryDescriptionProviderService filingHistoryDescriptionProviderService() {
            return new FilingHistoryDescriptionProviderService();
        }
    }

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

    @BeforeEach
    void setUp(){
        order = new OrderData();
        order.setReference("ORD-108815-904831");
        order.setPaymentReference("orderable_item_ORD-108815-904831");

        orderedBy = new ActionedBy();
        orderedBy.setEmail("demo@ch.gov.uk");
        order.setOrderedBy(orderedBy);

        delivery = new DeliveryDetails();
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

        item = new Item();
        item.setCompanyName("THE COMPANY");
        item.setCompanyNumber("00000001");
        item.setKind("item#certificate");
        options = new CertificateItemOptions();
        options.setDeliveryTimescale(DeliveryTimescale.STANDARD);
        options.setCertificateType(CertificateType.INCORPORATION_WITH_ALL_NAME_CHANGES);
    }

    @Test
    void orderToConfirmationBehavesAsExpected() {

        options.setIncludeGoodStandingInformation(true);
        final RegisteredOfficeAddressDetails officeDetails = new RegisteredOfficeAddressDetails();
        officeDetails.setIncludeAddressRecordsType(CURRENT);
        options.setRegisteredOfficeAddressDetails(officeDetails);
        final DirectorOrSecretaryDetails directors = new DirectorOrSecretaryDetails();
        directors.setIncludeBasicInformation(false);
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
        assertInformationIsAsExpected(confirmation);
        assertThat(confirmation.getCertificateGoodStandingInformation(), is("Yes"));
        assertThat(confirmation.getCertificateDirectors(), is("No"));
        assertThat(confirmation.getCertificateSecretaries(), is("Yes"));
        assertThat(confirmation.getCertificateCompanyObjects(), is("Yes"));
        assertThat(confirmation.getTimeOfPayment(), is(DATETIME_OF_PAYMENT_FORMATTER.format(order.getOrderedAt())));
        assertThat(confirmation.getFeeAmount(), is("15"));
    }

    @Test
    void orderToConfirmationBehavesAsExpectedNullDirectorDetails() {

        options.setIncludeGoodStandingInformation(true);
        final RegisteredOfficeAddressDetails officeDetails = new RegisteredOfficeAddressDetails();
        officeDetails.setIncludeAddressRecordsType(CURRENT);
        options.setRegisteredOfficeAddressDetails(officeDetails);
        options.setDirectorDetails(null);
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
        assertInformationIsAsExpected(confirmation);
        assertThat(confirmation.getCertificateGoodStandingInformation(), is("Yes"));
        assertThat(confirmation.getCertificateDirectors(), is("No"));
        assertThat(confirmation.getCertificateSecretaries(), is("Yes"));
        assertThat(confirmation.getCertificateCompanyObjects(), is("Yes"));
        assertThat(confirmation.getTimeOfPayment(), is(DATETIME_OF_PAYMENT_FORMATTER.format(order.getOrderedAt())));
        assertThat(confirmation.getFeeAmount(), is("15"));
    }

    @Test
    void orderToConfirmationBehavesAsExpectedNullSecretaries() {

        options.setIncludeGoodStandingInformation(true);
        final RegisteredOfficeAddressDetails officeDetails = new RegisteredOfficeAddressDetails();
        officeDetails.setIncludeAddressRecordsType(CURRENT);
        options.setRegisteredOfficeAddressDetails(officeDetails);
        final DirectorOrSecretaryDetails directors = new DirectorOrSecretaryDetails();
        directors.setIncludeBasicInformation(false);
        options.setDirectorDetails(directors);
        options.setSecretaryDetails(null);
        options.setIncludeCompanyObjectsInformation(true);

        item.setItemOptions(options);
        order.setItems(singletonList(item));
        order.setOrderedAt(LocalDateTime.now());
        order.setTotalOrderCost("15");

        // When
        final CertificateOrderConfirmation confirmation = mapperUnderTest.orderToConfirmation(order);
        assertInformationIsAsExpected(confirmation);
        assertThat(confirmation.getCertificateGoodStandingInformation(), is("Yes"));
        assertThat(confirmation.getCertificateDirectors(), is("No"));
        assertThat(confirmation.getCertificateSecretaries(), is("No"));
        assertThat(confirmation.getCertificateCompanyObjects(), is("Yes"));
        assertThat(confirmation.getTimeOfPayment(), is(DATETIME_OF_PAYMENT_FORMATTER.format(order.getOrderedAt())));
        assertThat(confirmation.getFeeAmount(), is("15"));
    }

    @Test
    void orderToConfirmationBehavesAsExpectedNullDirectorsDetails() {

        options.setIncludeGoodStandingInformation(true);
        final RegisteredOfficeAddressDetails officeDetails = new RegisteredOfficeAddressDetails();
        officeDetails.setIncludeAddressRecordsType(CURRENT);
        options.setRegisteredOfficeAddressDetails(officeDetails);
        final DirectorOrSecretaryDetails directors = new DirectorOrSecretaryDetails();
        directors.setIncludeBasicInformation(null);
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
        assertInformationIsAsExpected(confirmation);
        assertThat(confirmation.getCertificateGoodStandingInformation(), is("Yes"));
        assertThat(confirmation.getCertificateDirectors(), is("No"));
        assertThat(confirmation.getCertificateSecretaries(), is("Yes"));
        assertThat(confirmation.getCertificateCompanyObjects(), is("Yes"));
        assertThat(confirmation.getTimeOfPayment(), is(DATETIME_OF_PAYMENT_FORMATTER.format(order.getOrderedAt())));
        assertThat(confirmation.getFeeAmount(), is("15"));
    }

    @Test
    void orderToConfirmationCopesWithMissingDirectorAndSecretaryDetails() {

        options.setIncludeGoodStandingInformation(true);
        final RegisteredOfficeAddressDetails officeDetails = new RegisteredOfficeAddressDetails();
        officeDetails.setIncludeAddressRecordsType(CURRENT);
        options.setRegisteredOfficeAddressDetails(officeDetails);
        final DirectorOrSecretaryDetails directors = new DirectorOrSecretaryDetails();
        directors.setIncludeBasicInformation(false);
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
        assertInformationIsAsExpected(confirmation);
        assertThat(confirmation.getCertificateGoodStandingInformation(), is("Yes"));
        assertThat(confirmation.getCertificateDirectors(), is("No"));
        assertThat(confirmation.getCertificateSecretaries(), is("Yes"));
        assertThat(confirmation.getCertificateCompanyObjects(), is("Yes"));
        assertThat(confirmation.getTimeOfPayment(), is(DATETIME_OF_PAYMENT_FORMATTER.format(order.getOrderedAt())));
        assertThat(confirmation.getFeeAmount(), is("15"));
    }

    @Test
    void orderToConfirmationROOCurrentAddress() {
        options.setIncludeGoodStandingInformation(true);
        final RegisteredOfficeAddressDetails officeDetails = new RegisteredOfficeAddressDetails();
        officeDetails.setIncludeAddressRecordsType(CURRENT);
        options.setRegisteredOfficeAddressDetails(officeDetails);
        final DirectorOrSecretaryDetails directors = new DirectorOrSecretaryDetails();
        directors.setIncludeBasicInformation(false);
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
        assertInformationIsAsExpected(confirmation);
        assertThat(confirmation.getCertificateGoodStandingInformation(), is("Yes"));
        assertThat(confirmation.getCertificateRegisteredOfficeOptions(), is("Current address"));
        assertThat(confirmation.getCertificateDirectors(), is("No"));
        assertThat(confirmation.getCertificateSecretaries(), is("Yes"));
        assertThat(confirmation.getCertificateCompanyObjects(), is("Yes"));
        assertThat(confirmation.getTimeOfPayment(), is(DATETIME_OF_PAYMENT_FORMATTER.format(order.getOrderedAt())));
        assertThat(confirmation.getFeeAmount(), is("15"));
    }

    @Test
    void orderToConfirmationROOCurrentAddressAndOnePrevious() {

        options.setIncludeGoodStandingInformation(true);
        final RegisteredOfficeAddressDetails officeDetails = new RegisteredOfficeAddressDetails();
        officeDetails.setIncludeAddressRecordsType(CURRENT_AND_PREVIOUS);
        options.setRegisteredOfficeAddressDetails(officeDetails);
        final DirectorOrSecretaryDetails directors = new DirectorOrSecretaryDetails();
        directors.setIncludeBasicInformation(false);
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
        assertInformationIsAsExpected(confirmation);
        assertThat(confirmation.getCertificateGoodStandingInformation(), is("Yes"));
        assertThat(confirmation.getCertificateRegisteredOfficeOptions(), is("Current address and the one previous"));
        assertThat(confirmation.getCertificateDirectors(), is("No"));
        assertThat(confirmation.getCertificateSecretaries(), is("Yes"));
        assertThat(confirmation.getCertificateCompanyObjects(), is("Yes"));
        assertThat(confirmation.getTimeOfPayment(), is(DATETIME_OF_PAYMENT_FORMATTER.format(order.getOrderedAt())));
        assertThat(confirmation.getFeeAmount(), is("15"));
    }

    @Test
    void orderToConfirmationROOCurrentAddressAndTwoPrevious() {

        options.setIncludeGoodStandingInformation(true);
        final RegisteredOfficeAddressDetails officeDetails = new RegisteredOfficeAddressDetails();
        officeDetails.setIncludeAddressRecordsType(CURRENT_PREVIOUS_AND_PRIOR);
        options.setRegisteredOfficeAddressDetails(officeDetails);
        final DirectorOrSecretaryDetails directors = new DirectorOrSecretaryDetails();
        directors.setIncludeBasicInformation(false);
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
        assertInformationIsAsExpected(confirmation);
        assertThat(confirmation.getCertificateGoodStandingInformation(), is("Yes"));
        assertThat(confirmation.getCertificateRegisteredOfficeOptions(), is("Current address and the two previous"));
        assertThat(confirmation.getCertificateDirectors(), is("No"));
        assertThat(confirmation.getCertificateSecretaries(), is("Yes"));
        assertThat(confirmation.getCertificateCompanyObjects(), is("Yes"));
        assertThat(confirmation.getTimeOfPayment(), is(DATETIME_OF_PAYMENT_FORMATTER.format(order.getOrderedAt())));
        assertThat(confirmation.getFeeAmount(), is("15"));
    }

    @Test
    void orderToConfirmationROOAllAddresses() {

        options.setIncludeGoodStandingInformation(true);
        final RegisteredOfficeAddressDetails officeDetails = new RegisteredOfficeAddressDetails();
        officeDetails.setIncludeAddressRecordsType(ALL);
        options.setRegisteredOfficeAddressDetails(officeDetails);
        final DirectorOrSecretaryDetails directors = new DirectorOrSecretaryDetails();
        directors.setIncludeBasicInformation(false);
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
        assertInformationIsAsExpected(confirmation);
        assertThat(confirmation.getCertificateGoodStandingInformation(), is("Yes"));
        assertThat(confirmation.getCertificateRegisteredOfficeOptions(), is("All current and previous addresses"));
        assertThat(confirmation.getCertificateDirectors(), is("No"));
        assertThat(confirmation.getCertificateSecretaries(), is("Yes"));
        assertThat(confirmation.getCertificateCompanyObjects(), is("Yes"));
        assertThat(confirmation.getTimeOfPayment(), is(DATETIME_OF_PAYMENT_FORMATTER.format(order.getOrderedAt())));
        assertThat(confirmation.getFeeAmount(), is("15"));
    }

    @Test
    void orderToConfirmationROONoAddresses() {

        options.setIncludeGoodStandingInformation(true);
        final RegisteredOfficeAddressDetails officeDetails = new RegisteredOfficeAddressDetails();
        officeDetails.setIncludeAddressRecordsType(null);
        options.setRegisteredOfficeAddressDetails(officeDetails);
        final DirectorOrSecretaryDetails directors = new DirectorOrSecretaryDetails();
        directors.setIncludeBasicInformation(false);
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
        assertInformationIsAsExpected(confirmation);
        assertThat(confirmation.getCertificateGoodStandingInformation(), is("Yes"));
        assertThat(confirmation.getCertificateRegisteredOfficeOptions(), is("No"));
        assertThat(confirmation.getCertificateDirectors(), is("No"));
        assertThat(confirmation.getCertificateSecretaries(), is("Yes"));
        assertThat(confirmation.getCertificateCompanyObjects(), is("Yes"));
        assertThat(confirmation.getTimeOfPayment(), is(DATETIME_OF_PAYMENT_FORMATTER.format(order.getOrderedAt())));
        assertThat(confirmation.getFeeAmount(), is("15"));
    }

    @Test
    void orderToConfirmationDirectorsOptionsAllYes() {

        options.setIncludeGoodStandingInformation(true);
        final RegisteredOfficeAddressDetails officeDetails = new RegisteredOfficeAddressDetails();
        officeDetails.setIncludeAddressRecordsType(null);
        options.setRegisteredOfficeAddressDetails(officeDetails);
        final DirectorOrSecretaryDetails directors = new DirectorOrSecretaryDetails();
        directors.setIncludeBasicInformation(true);
        directors.setIncludeDobType(IncludeDobType.PARTIAL);
        directors.setIncludeAddress(true);
        directors.setIncludeAppointmentDate(true);
        directors.setIncludeCountryOfResidence(true);
        directors.setIncludeNationality(true);
        directors.setIncludeOccupation(true);
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
        assertInformationIsAsExpected(confirmation);
        assertThat(confirmation.getCertificateGoodStandingInformation(), is("Yes"));
        assertThat(confirmation.getCertificateRegisteredOfficeOptions(), is("No"));
        assertThat(confirmation.getCertificateDirectors(), is("Yes"));
        assertThat(directors.getIncludeDobType(), is(IncludeDobType.PARTIAL));
        assertThat(directors.getIncludeAddress(), is(true));
        assertThat(directors.getIncludeAppointmentDate(), is(true));
        assertThat(directors.getIncludeCountryOfResidence(), is(true));
        assertThat(directors.getIncludeNationality(), is(true));
        assertThat(directors.getIncludeOccupation(), is(true));
        assertThat(confirmation.getCertificateSecretaries(), is("Yes"));
        assertThat(confirmation.getCertificateCompanyObjects(), is("Yes"));
        assertThat(confirmation.getTimeOfPayment(), is(DATETIME_OF_PAYMENT_FORMATTER.format(order.getOrderedAt())));
        assertThat(confirmation.getFeeAmount(), is("15"));
    }

    @Test
    void orderToConfirmationDirectorsOptionsIsNotSelected() {

        options.setIncludeGoodStandingInformation(true);
        final RegisteredOfficeAddressDetails officeDetails = new RegisteredOfficeAddressDetails();
        officeDetails.setIncludeAddressRecordsType(null);
        options.setRegisteredOfficeAddressDetails(officeDetails);
        final DirectorOrSecretaryDetails directors = new DirectorOrSecretaryDetails();
        directors.setIncludeBasicInformation(false);
        directors.setIncludeAddress(false);
        directors.setIncludeAppointmentDate(false);
        directors.setIncludeCountryOfResidence(false);
        directors.setIncludeNationality(false);
        directors.setIncludeOccupation(false);
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
        assertInformationIsAsExpected(confirmation);
        assertThat(confirmation.getCertificateGoodStandingInformation(), is("Yes"));
        assertThat(confirmation.getCertificateRegisteredOfficeOptions(), is("No"));
        assertThat(confirmation.getCertificateDirectors(), is("No"));
        assertThat(directors.getIncludeAddress(), is(false));
        assertThat(directors.getIncludeAppointmentDate(), is(false));
        assertThat(confirmation.getCertificateSecretaries(), is("Yes"));
        assertThat(confirmation.getCertificateCompanyObjects(), is("Yes"));
        assertThat(confirmation.getTimeOfPayment(), is(DATETIME_OF_PAYMENT_FORMATTER.format(order.getOrderedAt())));
        assertThat(confirmation.getFeeAmount(), is("15"));
    }

    @Test
    void orderToConfirmationDirectorsOptionsMixOfChoices() {

        options.setIncludeGoodStandingInformation(true);
        final RegisteredOfficeAddressDetails officeDetails = new RegisteredOfficeAddressDetails();
        officeDetails.setIncludeAddressRecordsType(null);
        options.setRegisteredOfficeAddressDetails(officeDetails);
        final DirectorOrSecretaryDetails directors = new DirectorOrSecretaryDetails();
        directors.setIncludeBasicInformation(true);
        directors.setIncludeDobType(IncludeDobType.PARTIAL);
        directors.setIncludeAddress(true);
        directors.setIncludeAppointmentDate(false);
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
        assertInformationIsAsExpected(confirmation);
        assertThat(confirmation.getCertificateGoodStandingInformation(), is("Yes"));
        assertThat(confirmation.getCertificateRegisteredOfficeOptions(), is("No"));
        assertThat(confirmation.getCertificateDirectors(), is("Yes"));
        assertThat(directors.getIncludeDobType(), is(IncludeDobType.PARTIAL));
        assertThat(directors.getIncludeAddress(), is(true));
        assertThat(directors.getIncludeAppointmentDate(), is(false));
        assertThat(confirmation.getCertificateSecretaries(), is("Yes"));
        assertThat(confirmation.getCertificateCompanyObjects(), is("Yes"));
        assertThat(confirmation.getTimeOfPayment(), is(DATETIME_OF_PAYMENT_FORMATTER.format(order.getOrderedAt())));
        assertThat(confirmation.getFeeAmount(), is("15"));
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

    @Test
    void includesCurrentRegisteredOfficeAddress() {

        // Given
        final Item item = new Item();
        item.setKind("item#certificate");
        final CertificateItemOptions options = new CertificateItemOptions();
        final RegisteredOfficeAddressDetails officeDetails = new RegisteredOfficeAddressDetails();
        officeDetails.setIncludeAddressRecordsType(CURRENT);
        options.setRegisteredOfficeAddressDetails(officeDetails);
        item.setItemOptions(options);

        // When
        final String[] includes =  mapperUnderTest.getCertificateIncludes(item);

        // Then
        assertThat(asList(includes), contains("Registered office address"));
    }

    @Test
    void doesNotIncludeCurrentAndPreviousRegisteredOfficeAddress() {

        // Given
        final Item item = new Item();
        item.setKind("item#certificate");
        final CertificateItemOptions options = new CertificateItemOptions();
        final RegisteredOfficeAddressDetails officeDetails = new RegisteredOfficeAddressDetails();
        officeDetails.setIncludeAddressRecordsType(CURRENT_AND_PREVIOUS);
        options.setRegisteredOfficeAddressDetails(officeDetails);
        item.setItemOptions(options);

        // When
        final String[] includes =  mapperUnderTest.getCertificateIncludes(item);

        // Then
        assertThat(asList(includes), not(contains("Registered office address")));

    }

    @Test
    void doesNotIncludeRegisteredOfficeAddressWithNoRecordsType() {
        // Given
        final Item item = new Item();
        item.setKind("item#certificate");
        final CertificateItemOptions options = new CertificateItemOptions();
        final RegisteredOfficeAddressDetails officeDetails = new RegisteredOfficeAddressDetails();
        options.setRegisteredOfficeAddressDetails(officeDetails);
        item.setItemOptions(options);

        // When
        final String[] includes =  mapperUnderTest.getCertificateIncludes(item);

        // Then
        assertThat(asList(includes), not(contains("Registered office address")));
    }

    @Test
    void incorporationWithAllNameChangesHasASpecialLabel() {
        for (final CertificateType type : CertificateType.values()) {
            if (type == CertificateType.INCORPORATION_WITH_ALL_NAME_CHANGES) {
                assertThat(mapperUnderTest.getCertificateType(type), is("Incorporation with all company name changes"));
            } else {
                assertThat(mapperUnderTest.getCertificateType(type), is(mapperUnderTest.toSentenceCase(type.toString())));
            }
        }
    }

    void assertInformationIsAsExpected(CertificateOrderConfirmation confirmation){

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
        assertThat(confirmation.getCertificateType(), is("Incorporation with all company name changes"));
    }

}
