package uk.gov.companieshouse.itemhandler.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.itemhandler.service.ChdItemSenderService;
import uk.gov.companieshouse.itemhandler.service.EmailService;

import static java.util.Arrays.stream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static uk.gov.companieshouse.itemhandler.model.ItemType.CERTIFICATE;
import static uk.gov.companieshouse.itemhandler.model.ItemType.CERTIFIED_COPY;
import static uk.gov.companieshouse.itemhandler.model.ItemType.SCAN_ON_DEMAND;

/**
 * Integration tests the {@link ItemType} enum.
 */
@SpringBootTest
class ItemTypeIntegrationTest {

    private static final String UNKNOWN_KIND = "item#unknown";

    @MockBean
    private EmailService emailer;

    @MockBean
    private ChdItemSenderService itemSender;

    @Mock
    private OrderData order;

    @Configuration
    @ComponentScan(basePackageClasses = ItemTypeIntegrationTest.class)
    static class Config { }

    @Test
    @DisplayName("Gets the correct item type for any known kind")
    void getItemTypeGetsExpectedItemType() {
        stream(ItemType.values()).forEach(type->
                assertThat(ItemType.getItemType(type.getKind()), is(type)));
    }

    @Test
    @DisplayName("Gets nothing for an unknown kind")
    void getItemTypeGetsNothingForUnknownKind() {
        assertThat(ItemType.getItemType(UNKNOWN_KIND), is(nullValue()));
    }

    @Test
    @DisplayName("Certificate order sent to email")
    void certificateOrderSentToEmail() throws Exception {

        // When
        CERTIFICATE.sendMessages(order);

        // Then
        verify(emailer).sendOrderConfirmation(order);

    }

    @Test
    @DisplayName("Certified copy order sent to email")
    void certifiedCopyOrderSentToEmail() throws Exception {

        // When
        CERTIFIED_COPY.sendMessages(order);

        // Then
        verify(emailer).sendOrderConfirmation(order);

    }

    @Test
    @DisplayName("Scan upon demand order sent to CHD")
    void scanUponDemandOrderSentToChd() throws Exception {

        // When
        SCAN_ON_DEMAND.sendMessages(order);

        // Then
        verify(itemSender).sendItemsToChd(order);

    }

}
