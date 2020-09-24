package uk.gov.companieshouse.itemhandler.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ValueMapping;
import uk.gov.companieshouse.api.model.order.OrdersApi;
import uk.gov.companieshouse.api.model.order.item.BaseItemApi;
import uk.gov.companieshouse.api.model.order.item.BaseItemOptionsApi;
import uk.gov.companieshouse.api.model.order.item.CertificateItemOptionsApi;
import uk.gov.companieshouse.api.model.order.item.CertifiedCopyItemOptionsApi;
import uk.gov.companieshouse.api.model.order.item.ProductTypeApi;
import uk.gov.companieshouse.itemhandler.model.CertificateItemOptions;
import uk.gov.companieshouse.itemhandler.model.CertifiedCopyItemOptions;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.ItemType;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.itemhandler.model.ProductType;

@Mapper(componentModel = "spring")
public interface OrdersApiToOrderDataMapper {
    OrderData ordersApiToOrderData(OrdersApi ordersApi);

    @Mapping(source = "links.self", target="itemUri")
    @Mapping(target = "satisfiedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    Item apiToItem(BaseItemApi baseItemApi);

    // TODO GCI-1373 Remove this special case mapping when private SDK is updated for missing image delivery.
    @ValueMapping(source = "SCAN_UPON_DEMAND", target = "MISSING_IMAGE_DELIVERY")
    ProductType apiToProductType(ProductTypeApi productTypeApi);

    /**
     * Maps item_options based on description_identifier correctly to
     * {@link CertificateItemOptions} or {@link CertifiedCopyItemOptions}
     * @param baseItemApi item object received via api call
     * @param item item object to be constructed from item received via api call
     */
    @AfterMapping
    default void apiToItemOptions(BaseItemApi baseItemApi, @MappingTarget Item item) {
        final String itemKind = baseItemApi.getKind();
        final BaseItemOptionsApi baseItemOptionsApi = baseItemApi.getItemOptions();
        if (itemKind.equals(ItemType.CERTIFICATE.getKind())) {
            item.setItemOptions(apiToCertificateItemOptions((CertificateItemOptionsApi) baseItemOptionsApi));
        }
        else {
            item.setItemOptions(apiToCertifiedCopyItemOptions((CertifiedCopyItemOptionsApi) baseItemOptionsApi));
        }
    }

    CertificateItemOptions apiToCertificateItemOptions(CertificateItemOptionsApi certificateItemOptionsApi);
    CertifiedCopyItemOptions apiToCertifiedCopyItemOptions(CertifiedCopyItemOptionsApi certifiedCopyItemOptionsApi);
}
