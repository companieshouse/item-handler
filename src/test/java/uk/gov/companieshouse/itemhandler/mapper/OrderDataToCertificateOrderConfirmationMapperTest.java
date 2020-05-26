package uk.gov.companieshouse.itemhandler.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.itemhandler.email.CertificateOrderConfirmation;
import uk.gov.companieshouse.itemhandler.model.OrderData;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests the {@link OrderDataToCertificateOrderConfirmationMapper} interface default method implementations.
 */
public class OrderDataToCertificateOrderConfirmationMapperTest {

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

    private TestOrderDataToCertificateOrderConfirmationMapper mapperUnderTest;

    @BeforeEach
    void setUp() {
        mapperUnderTest = new TestOrderDataToCertificateOrderConfirmationMapper();
    }

    @Test
    void toSentenceCaseBehavesAsExpected() {
        assertThat(mapperUnderTest.toSentenceCase("INCORPORATION_WITH_ALL_NAME_CHANGES"), is("Incorporation with all name changes"));
        assertThat(mapperUnderTest.toSentenceCase("STANDARD delivery"), is("Standard delivery"));
        assertThat(mapperUnderTest.toSentenceCase("SAME_DAY delivery"), is("Same day delivery"));
    }

}
