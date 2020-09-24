package uk.gov.companieshouse.itemhandler.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.ItemType;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.itemhandler.service.OrderProcessorService;
import uk.gov.companieshouse.itemhandler.service.OrderRouterService;
import uk.gov.companieshouse.logging.Logger;

import java.security.SecureRandom;
import java.util.Calendar;

import static java.util.Collections.singletonList;
import static uk.gov.companieshouse.itemhandler.model.ItemType.MISSING_IMAGE_DELIVERY;

/**
 * Temporary controller introduced to facilitate testing.
 * TODO GCI-1460 Remove this temporary test code.
 */
@RestController
public class TestController {

    private static final Logger LOGGER = LoggingUtils.getLogger();

    private static final String ITEM_KIND_PREFIX = "item#";
    private static final String ORDERS_URI_PREFIX = "/orders/";

    private final OrderProcessorService orderProcessor;
    private final OrderRouterService orderRouter;

    public TestController(final OrderProcessorService orderProcessor,
                          final OrderRouterService orderRouter) {
        this.orderProcessor = orderProcessor;
        this.orderRouter = orderRouter;
    }

    @PutMapping("${uk.gov.companieshouse.item-handler.test}/{kind}/{order}")
    public ResponseEntity<Void> testProcessing (final @PathVariable String kind,
                                                final @PathVariable String order) throws Exception {
        LOGGER.info("testProcessing (" + kind + ", " + order + ")");
        if (isMissingImageDelivery(kind)) {
            stubMissingImageDeliveryOrderAndRouteIt(order);
        } else {
            subjectOrderToFullProcessing(order);
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * Determines whether the kind represents a missing image delivery item (<code>true</code>), or not (<code>false</code>).
     * @param kind the partial kind string provided in the test (<code>certificate</code>, <code>certified-copy</code>
     *             or <code>missing-image-delivery</code>)
     * @return whether the kind represents a missing image delivery item (<code>true</code>), or not (<code>false</code>)
     */
    private boolean isMissingImageDelivery(final String kind) {
        return ItemType.getItemType(ITEM_KIND_PREFIX + kind) == MISSING_IMAGE_DELIVERY;
    }

    /**
     * Tests the full processing of the order that Item Handler implements, equivalent to the processing it performs
     * once it has consumed the order from the <code>order-received</code> topic. This assumes that the order identified
     * has already been successfully paid for at this point.
     * @param order the order reference identifying the order
     */
    private void subjectOrderToFullProcessing(final String order) {
        orderProcessor.processOrderReceived(ORDERS_URI_PREFIX + order);
    }

    /**
     * Creates a dummy or stub {@link OrderData} instance intended to be a minimal, transient representation of a
     * missing image delivery order as would be retrieved by the Item Handler from the Orders API were it possible for
     * the handler to do so at this point, then subjects the order to the same routing behaviour as the order would be
     * subject to if it had been retrieved from the Orders API. This effectively by-passes the order processing
     * behaviour that depends on successful retrieval of paid orders from the Orders API.
     * @param order the order reference identifying the order
     * @throws Exception should something unexpected happen
     */
    @SuppressWarnings("squid:S112") // in this test code, exception thrown is not important
    private void stubMissingImageDeliveryOrderAndRouteIt(final String order) throws Exception {
        final OrderData missingImageDeliveryOrder = stubMissingImageDeliveryOrder(order);
        orderRouter.routeOrder(missingImageDeliveryOrder);
    }

    /**
     * Stubs out just enough of a missing image delivery order to be able to test its routing by this Item Handler
     * application.
     * @param order the order reference identifying the order
     * @return the stub {@link OrderData} instance
     */
    private OrderData stubMissingImageDeliveryOrder(final String order) {
        final OrderData missingImageDeliveryOrder = new OrderData();
        missingImageDeliveryOrder.setReference(order);
        final Item item = new Item();
        item.setId(generateMissingImageDeliveryItemId());
        item.setKind(MISSING_IMAGE_DELIVERY.getKind());
        missingImageDeliveryOrder.setItems(singletonList(item));
        return missingImageDeliveryOrder;
    }

    /**
     * Uses the same code as the Missing Image Delivery API to generate a unique ID for each missing image delivery item.
     * @return a unique missing image delivery item ID
     */
    private String generateMissingImageDeliveryItemId() {
        final SecureRandom random = new SecureRandom();
        final byte[] values = new byte[4];
        random.nextBytes(values);
        final String rand = String.format("%04d", random.nextInt(9999));
        final String time = String.format("%08d", Calendar.getInstance().getTimeInMillis() / 100000L);
        final String rawId = rand + time;
        final String[] tranId = rawId.split("(?<=\\G.{6})");
        return "MID-" + String.join("-", tranId);
    }

}
