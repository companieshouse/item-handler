package uk.gov.companieshouse.itemhandler.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import uk.gov.companieshouse.api.model.order.OrdersApi;
import uk.gov.companieshouse.api.model.order.item.BaseItemApi;
import uk.gov.companieshouse.api.model.order.item.BaseItemOptionsApi;
import uk.gov.companieshouse.api.model.order.item.CertificateItemOptionsApi;
import uk.gov.companieshouse.api.model.order.item.CertifiedCopyItemOptionsApi;
import uk.gov.companieshouse.itemhandler.model.CertificateItemOptions;
import uk.gov.companieshouse.itemhandler.model.CertifiedCopyItemOptions;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.ItemOptions;
import uk.gov.companieshouse.itemhandler.model.ItemType;
import uk.gov.companieshouse.itemhandler.model.OrderData;

@Mapper(componentModel = "spring")
public interface OrdersApiToOrderDataMapper {
    OrderData ordersApiToOrderData(OrdersApi ordersApi);

    @Mapping(source = "id", target="id")
    @Mapping(source = "companyName", target="companyName")
    @Mapping(source = "companyNumber", target="companyNumber")
    @Mapping(source = "customerReference", target="customerReference")
    @Mapping(source = "description", target="description")
    @Mapping(source = "descriptionIdentifier", target="descriptionIdentifier")
    @Mapping(source = "descriptionValues", target="descriptionValues")
    @Mapping(source = "itemCosts", target="itemCosts")
    @Mapping(source = "etag", target="etag")
    @Mapping(source = "kind", target="kind")
    @Mapping(source = "postalDelivery", target="postalDelivery")
    @Mapping(source = "quantity", target="quantity")
    @Mapping(source = "links", target="links")
    @Mapping(source = "postageCost", target="postageCost")
    @Mapping(source = "totalItemCost", target="totalItemCost")
    @Mapping(source = "links.self", target="itemUri")
    @Mapping(target = "satisfiedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    Item apiToItem(BaseItemApi baseItemApi);

    /**
     * Maps item_options based on description_identifier correctly to
     * {@link CertificateItemOptions} or {@link CertifiedCopyItemOptions}
     * @param baseItemApi item object received via api call
     * @param item item object to be constructed from item received via api call
     */
    @AfterMapping
    default void apiToItemOptions(BaseItemApi baseItemApi, @MappingTarget Item item) {
        item.setItemOptions(
                mapToSpecificItemOptions(baseItemApi.getItemOptions(), baseItemApi.getKind())
        );
    }

    default ItemOptions mapToSpecificItemOptions(BaseItemOptionsApi baseItemOptionsApi, String kind) {
        if (kind.equals(ItemType.CERTIFICATE.getKind())) {
            return apiToCertificateItemOptions((CertificateItemOptionsApi) baseItemOptionsApi);
        }
        else {
            return apiToCertifiedCopyItemOptions((CertifiedCopyItemOptionsApi) baseItemOptionsApi);
        }
    }

    CertificateItemOptions apiToCertificateItemOptions(CertificateItemOptionsApi certificateItemOptionsApi);
    CertifiedCopyItemOptions apiToCertifiedCopyItemOptions(CertifiedCopyItemOptionsApi certifiedCopyItemOptionsApi);
}
