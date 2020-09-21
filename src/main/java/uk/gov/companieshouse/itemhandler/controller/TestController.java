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

import static java.util.Collections.singletonList;
import static uk.gov.companieshouse.itemhandler.model.ItemType.SCAN_ON_DEMAND;

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
        if (isScanOnDemand(kind)) {
            stubScanOnDemandOrderAndRouteIt(order);
        } else {
            subjectOrderToFullProcessing(order);
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    private boolean isScanOnDemand(final String kind) {
        return ItemType.getItemType(ITEM_KIND_PREFIX + kind) == SCAN_ON_DEMAND;
    }

    private void subjectOrderToFullProcessing(final String order) {
        orderProcessor.processOrderReceived(ORDERS_URI_PREFIX + order);
    }

    @SuppressWarnings("squid:S112") // in this test code, exception thrown is not important
    private void stubScanOnDemandOrderAndRouteIt(final String order) throws Exception {
        final OrderData scanOnDemandOrder = stubScanOnDemandOrder(order);
        orderRouter.routeOrder(scanOnDemandOrder);
    }

    private OrderData stubScanOnDemandOrder(final String order) {
        final OrderData scanUponDemandOrder = new OrderData();
        scanUponDemandOrder.setReference(order);
        final Item item = new Item();
        item.setKind(SCAN_ON_DEMAND.getKind());
        scanUponDemandOrder.setItems(singletonList(item));
        return scanUponDemandOrder;
    }

}
