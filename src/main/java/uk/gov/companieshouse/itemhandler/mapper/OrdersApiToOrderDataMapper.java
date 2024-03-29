package uk.gov.companieshouse.itemhandler.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ValueMapping;
import org.mapstruct.ValueMappings;
import uk.gov.companieshouse.api.model.order.OrdersApi;
import uk.gov.companieshouse.api.model.order.item.BaseItemApi;
import uk.gov.companieshouse.api.model.order.item.BaseItemOptionsApi;
import uk.gov.companieshouse.api.model.order.item.CertificateItemOptionsApi;
import uk.gov.companieshouse.api.model.order.item.CertifiedCopyItemOptionsApi;
import uk.gov.companieshouse.api.model.order.item.MissingImageDeliveryItemOptionsApi;
import uk.gov.companieshouse.itemhandler.model.CertificateItemOptions;
import uk.gov.companieshouse.itemhandler.model.CertifiedCopyItemOptions;
import uk.gov.companieshouse.itemhandler.model.CompanyStatus;
import uk.gov.companieshouse.itemhandler.model.CompanyType;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.ItemOptions;
import uk.gov.companieshouse.itemhandler.model.MissingImageDeliveryItemOptions;
import uk.gov.companieshouse.itemhandler.model.OrderData;

@Mapper(componentModel = "spring")
public interface OrdersApiToOrderDataMapper {
    OrderData ordersApiToOrderData(OrdersApi ordersApi);

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
        final String itemKind = baseItemApi.getKind();
        final BaseItemOptionsApi baseItemOptionsApi = baseItemApi.getItemOptions();
        if ("item#certificate".equals(itemKind)) {
            item.setItemOptions(apiToCertificateItemOptions((CertificateItemOptionsApi) baseItemOptionsApi));
        }
        else if ("item#certified-copy".equals(itemKind)){
            item.setItemOptions(apiToCertifiedCopyItemOptions((CertifiedCopyItemOptionsApi) baseItemOptionsApi));
        } else {
            item.setItemOptions(apiToMissingImageDeliveryOptions((MissingImageDeliveryItemOptionsApi) baseItemOptionsApi));
        }
    }

    ItemOptions apiToOptions(BaseItemOptionsApi baseItemOptionsApi);
    @Mapping(source = "designatedMemberDetails", target = "designatedMembersDetails")
    @Mapping(source = "memberDetails", target = "membersDetails")
    CertificateItemOptions apiToCertificateItemOptions(CertificateItemOptionsApi certificateItemOptionsApi);
    CertifiedCopyItemOptions apiToCertifiedCopyItemOptions(CertifiedCopyItemOptionsApi certifiedCopyItemOptionsApi);
    MissingImageDeliveryItemOptions apiToMissingImageDeliveryOptions(MissingImageDeliveryItemOptionsApi missingImageDeliveryItemOptionsApi);
    @ValueMappings({
        @ValueMapping(source = "llp", target = "LIMITED_LIABILITY_PARTNERSHIP"),
        @ValueMapping(source = "limited-partnership", target = "LIMITED_PARTNERSHIP"),
        @ValueMapping(source = MappingConstants.ANY_UNMAPPED, target = "OTHER")
    })
    CompanyType mapCompanyType(String companyType);

    @ValueMappings({
            @ValueMapping(source = "active", target = "ACTIVE"),
            @ValueMapping(source = "liquidation", target = "LIQUIDATION"),
            @ValueMapping(source = MappingConstants.ANY_UNMAPPED, target = "OTHER")
    })
    CompanyStatus mapCompanyStatus(String companyStatus);

}
