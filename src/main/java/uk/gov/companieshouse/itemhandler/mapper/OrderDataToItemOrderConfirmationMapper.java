package uk.gov.companieshouse.itemhandler.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.companieshouse.itemhandler.email.ItemDetails;
import uk.gov.companieshouse.itemhandler.email.ItemOrderConfirmation;
import uk.gov.companieshouse.itemhandler.model.CertifiedCopyItemOptions;
import uk.gov.companieshouse.itemhandler.model.FilingHistoryDocument;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.ItemCosts;
import uk.gov.companieshouse.itemhandler.model.MissingImageDeliveryItemOptions;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.itemhandler.service.FilingHistoryDescriptionProviderService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static uk.gov.companieshouse.itemhandler.util.DateConstants.DATE_FILED_FORMATTER;

@Mapper(componentModel = "spring")
public abstract class OrderDataToItemOrderConfirmationMapper implements MapperUtil {

    @Value("${dispatch-days}")
    private String dispatchDays;

    @Autowired
    private FilingHistoryDescriptionProviderService filingHistoryDescriptionProviderService;

    // Name/address mappings
    @Mapping(source = "deliveryDetails.forename", target="forename")
    @Mapping(source = "deliveryDetails.surname", target="surname")
    @Mapping(source = "deliveryDetails.addressLine1", target="addressLine1")
    @Mapping(source = "deliveryDetails.addressLine2", target="addressLine2")
    @Mapping(source = "deliveryDetails.locality", target="houseName")
    @Mapping(source = "deliveryDetails.premises", target="houseNumberStreetName")
    @Mapping(source = "deliveryDetails.region", target="city")
    @Mapping(source = "deliveryDetails.postalCode", target="postCode")
    @Mapping(source = "deliveryDetails.country", target="country")

    // Order details field mappings
    @Mapping(source = "reference", target="orderReferenceNumber")
    @Mapping(source = "orderedBy.email", target="emailAddress")
    @Mapping(source = "totalOrderCost", target="totalFee")
    public abstract ItemOrderConfirmation orderToConfirmation(OrderData order);

    @AfterMapping
    public void mapCertifiedCopyOrMissingImageDeliveryItems(final OrderData order,
                                                            final @MappingTarget ItemOrderConfirmation confirmation) {
        final Item item = order.getItems().get(0);

        confirmation.setCompanyName(item.getCompanyName());
        confirmation.setCompanyNumber(item.getCompanyNumber());

        if (item.getKind().equals("item#certified-copy")) {
            final String timescale =
                    ((CertifiedCopyItemOptions) item.getItemOptions()).getDeliveryTimescale().toString();
            if (timescale.equals("SAME_DAY")){
                confirmation.setDeliveryMethod("Express (Orders received before 11am will be dispatched the same day. Orders received after 11am will be dispatched the next working day)");
            } else {
                String deliveryMethod = String.format("%s delivery (aim to dispatch within %s working days)", toSentenceCase(timescale), dispatchDays);
                confirmation.setDeliveryMethod(deliveryMethod);
            }
            confirmation.setItemDetails(collateItemDetailsForCertifiedCopy(item));
        }
        else {
            confirmation.setItemDetails(collateItemDetailsForMissingImageDelivery(item));
        }

        confirmation.setTimeOfPayment(getTimeOfPayment(order.getOrderedAt()));
    }

    public List<ItemDetails> collateItemDetailsForMissingImageDelivery(Item item) {
        MissingImageDeliveryItemOptions midItemOptions = (MissingImageDeliveryItemOptions) item.getItemOptions();
        List<ItemCosts> itemCosts = item.getItemCosts();
        List<ItemDetails> itemDetails = new ArrayList<>();
        ItemDetails details = new ItemDetails();
        details.setDateFiled(reformatDateFiled(midItemOptions.getFilingHistoryDate()));
        details.setDescription(filingHistoryDescriptionProviderService.mapFilingHistoryDescription(
            midItemOptions.getFilingHistoryDescription(),
            midItemOptions.getFilingHistoryDescriptionValues()
        ));
        details.setType(midItemOptions.getFilingHistoryType());
        details.setFee(itemCosts.get(0).getCalculatedCost());
        itemDetails.add(details);

        return itemDetails;
    }

    public List<ItemDetails> collateItemDetailsForCertifiedCopy(Item item) {
        CertifiedCopyItemOptions itemOptions = (CertifiedCopyItemOptions) item.getItemOptions();
        List<FilingHistoryDocument> filingHistoryDocuments = itemOptions.getFilingHistoryDocuments();
        List<ItemCosts> itemCosts = item.getItemCosts();

        List<ItemDetails> itemDetails = new ArrayList<>();
        IntStream.range(0, filingHistoryDocuments.size()).forEach(i -> {
            ItemDetails details = new ItemDetails();
            details.setDateFiled(reformatDateFiled(filingHistoryDocuments.get(i).getFilingHistoryDate()));
            details.setType(filingHistoryDocuments.get(i).getFilingHistoryType());
            details.setDescription(
                    filingHistoryDescriptionProviderService.mapFilingHistoryDescription(
                            filingHistoryDocuments.get(i).getFilingHistoryDescription(),
                            filingHistoryDocuments.get(i).getFilingHistoryDescriptionValues()));
            details.setFee(itemCosts.get(i).getCalculatedCost());
            itemDetails.add(details);
        });

        return itemDetails;
    }

    /**
     * Reformats a date filed string such as "2009-08-23" as "23 Aug 2009".
     * @param dateFiled the date filed as reported from the filing history
     * @return the same date rendered for display purposes
     */
    @Named("reformatDateFiled")
    public String reformatDateFiled(final String dateFiled) {
        final LocalDate parsedDate = LocalDate.parse(dateFiled);
        return parsedDate.format(DATE_FILED_FORMATTER);
    }
}
