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

import java.time.LocalDateTime;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests the {@link OrderDataToCertificateOrderConfirmationMapper} interface and its implementation.
 */
@ExtendWith(SpringExtension.class)
@SpringJUnitConfig(OrderDataToCertificateOrderConfirmationMapperTest.Config.class)
public class OrderDataToCertificateOrderConfirmationMapperTest {

    private static final String[] EXPECTED_CERTIFICATE_INCLUDES = new String[]{
            "Statement of good standing",
            "Registered office address",
            "Directors",
            "Secretaries",
            "Company objects"
    };

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

    // TODO GCI-931 Complete test implementation
    @Test
    void orderToConfirmationBehavesAsExpected() {
        final OrderData order = new OrderData();
        final DeliveryDetails delivery = new DeliveryDetails();
        delivery.setForename("Jenny");
        delivery.setSurname("Wilson");
        order.setDeliveryDetails(delivery);
        final Item item = new Item();
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

        final CertificateOrderConfirmation confirmation = mapperUnderTest.orderToConfirmation(order);
        assertThat(confirmation.getForename(), is("Jenny"));
        assertThat(confirmation.getSurname(), is("Wilson"));
        assertThat(confirmation.getDeliveryMethod(), is("Standard delivery"));
        assertThat(confirmation.getCertificateType(), is("Incorporation with all name changes"));
        assertThat(confirmation.getCertificateIncludes(), is(EXPECTED_CERTIFICATE_INCLUDES));
    }

    @Test
    void toSentenceCaseBehavesAsExpected() {
        final TestOrderDataToCertificateOrderConfirmationMapper mapperUnderTest =
                new TestOrderDataToCertificateOrderConfirmationMapper();
        assertThat(mapperUnderTest.toSentenceCase("INCORPORATION_WITH_ALL_NAME_CHANGES"), is("Incorporation with all name changes"));
        assertThat(mapperUnderTest.toSentenceCase("STANDARD delivery"), is("Standard delivery"));
        assertThat(mapperUnderTest.toSentenceCase("SAME_DAY delivery"), is("Same day delivery"));
    }

}
