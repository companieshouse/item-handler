package uk.gov.companieshouse.itemhandler.itemsummary;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.logging.Logger;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.itemhandler.util.TestConstants.KIND_CERTIFICATE;
import static uk.gov.companieshouse.itemhandler.util.TestConstants.KIND_CERTIFIED_COPY;
import static uk.gov.companieshouse.itemhandler.util.TestConstants.KIND_MISSING_IMAGE_DELIVERY;


/**
 * Unit tests the {@link DigitalOrderItemRouter} class.
 */
@ExtendWith(MockitoExtension.class)
class DigitalOrderItemRouterTest {

    @InjectMocks
    private DigitalOrderItemRouter routerUnderTest;

    @Mock
    private OrderData order;

    @Mock
    private Logger logger;

    @Mock Item item;

    @Test
    @DisplayName("Digital certificate is created")
    void routeDigitalCertificate() {

        when(order.getItems()).thenReturn(singletonList(item));
        when(item.getKind()).thenReturn(KIND_CERTIFICATE);
        when(item.isPostalDelivery()).thenReturn(false);

        final List<ItemGroup> groups = routerUnderTest.createItemGroups(order);
        assertThat(groups.size(), is(1));
        assertThat(groups.getFirst().getItems().size(), is(1));
        assertThat(groups.getFirst().getItems().getFirst().getKind(), is(KIND_CERTIFICATE));
    }

    @Test
    @DisplayName("Postal certificate is not created")
    void routePostalCertificate() {

        when(order.getItems()).thenReturn(singletonList(item));
        when(item.getKind()).thenReturn(KIND_CERTIFICATE);
        when(item.isPostalDelivery()).thenReturn(true);

        final List<ItemGroup> groups = routerUnderTest.createItemGroups(order);
        assertThat(groups.isEmpty(), is(true));
    }

    @Test
    @DisplayName("Digital certified copy is created")
    void routeDigitalCertifiedCopy() {

        when(order.getItems()).thenReturn(singletonList(item));
        when(item.getKind()).thenReturn(KIND_CERTIFIED_COPY);
        when(item.isPostalDelivery()).thenReturn(false);

        final List<ItemGroup> groups = routerUnderTest.createItemGroups(order);
        assertThat(groups.size(), is(1));
        assertThat(groups.getFirst().getItems().size(), is(1));
        assertThat(groups.getFirst().getItems().getFirst().getKind(), is(KIND_CERTIFIED_COPY));
    }

    @Test
    @DisplayName("Postal certified copy is not created")
    void routePostalCertifiedCopy() {

        when(order.getItems()).thenReturn(singletonList(item));
        when(item.getKind()).thenReturn(KIND_CERTIFIED_COPY);
        when(item.isPostalDelivery()).thenReturn(true);

        final List<ItemGroup> groups = routerUnderTest.createItemGroups(order);
        assertThat(groups.isEmpty(), is(true));
    }

    @Test
    @DisplayName("Missing image delivery is not created")
    void routeMissingImageDelivery() {

        when(order.getItems()).thenReturn(singletonList(item));
        when(item.getKind()).thenReturn(KIND_MISSING_IMAGE_DELIVERY);

        final List<ItemGroup> groups = routerUnderTest.createItemGroups(order);
        assertThat(groups.isEmpty(), is(true));
    }

    @Test
    @DisplayName("Mixed order is handled correctly")
    void routeMixedOrder() {

        final OrderData order  = new OrderData();
        order.setItems(asList(
                createItem("CRT-1", KIND_CERTIFICATE, true),
                createItem("CRT-2", KIND_CERTIFICATE, true),
                createItem("CRT-3", KIND_CERTIFICATE, false),
                createItem("CC-1", KIND_CERTIFIED_COPY, true),
                createItem("CC-2", KIND_CERTIFIED_COPY, false),
                createItem("CC-3", KIND_CERTIFIED_COPY, false),
                createItem("MID-1", KIND_MISSING_IMAGE_DELIVERY, true),
                createItem("MID-2", KIND_MISSING_IMAGE_DELIVERY, false))
        );

        final List<ItemGroup> groups = routerUnderTest.createItemGroups(order);
        assertThat(groups.isEmpty(), is(false));
        assertThat(groups.size(), is(3));

        final ItemGroup IG1 = groups.getFirst();
        assertThat(IG1.getKind(), is(KIND_CERTIFICATE));
        assertThat(IG1.getItems().size(), is(1));
        assertThat(IG1.getItems().getFirst().getId(), is("CRT-1"));

        final ItemGroup IG2 = groups.get(1);
        assertThat(IG2.getKind(), is(KIND_CERTIFICATE));
        assertThat(IG2.getItems().size(), is(1));
        assertThat(IG2.getItems().getFirst().getId(), is("CRT-2"));

        final ItemGroup IG3 = groups.get(2);
        assertThat(IG3.getKind(), is(KIND_CERTIFIED_COPY));
        assertThat(IG3.getItems().size(), is(1));
        assertThat(IG3.getItems().getFirst().getId(), is("CC-1"));
    }

    private Item createItem(final String id, final String kind, final boolean isDigital) {
        final Item item = new Item();
        item.setId(id);
        item.setKind(kind);
        item.setPostalDelivery(!isDigital);
        return item;
    }

}
