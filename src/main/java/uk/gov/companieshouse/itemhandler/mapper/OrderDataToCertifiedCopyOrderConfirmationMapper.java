package uk.gov.companieshouse.itemhandler.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.mapstruct.Named;
import uk.gov.companieshouse.itemhandler.email.CertifiedCopyOrderConfirmation;
import uk.gov.companieshouse.itemhandler.email.CertifiedDocument;
import uk.gov.companieshouse.itemhandler.model.CertifiedCopyItemOptions;
import uk.gov.companieshouse.itemhandler.model.FilingHistoryDocument;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.ItemCosts;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.itemhandler.service.FilingHistoryDescriptionProviderService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static uk.gov.companieshouse.itemhandler.mapper.OrderDataToOrderConfirmationMapperConstants.DATE_FILED_FORMATTER;

@Mapper(componentModel = "spring")
public abstract class OrderDataToCertifiedCopyOrderConfirmationMapper implements MapperUtil {

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
    public abstract CertifiedCopyOrderConfirmation orderToConfirmation(OrderData order);

    @AfterMapping
    public void mapCertifiedCopyItems(final OrderData order,
                                       final @MappingTarget CertifiedCopyOrderConfirmation confirmation) {
        final Item item = order.getItems().get(0);
        final String timescale = item.getItemOptions().getDeliveryTimescale().toString();

        confirmation.setCompanyName(item.getCompanyName());
        confirmation.setCompanyNumber(item.getCompanyNumber());
        String deliveryMethod = String.format("%s delivery (aim to dispatch within 4 working days)", toSentenceCase(timescale));
        confirmation.setDeliveryMethod(deliveryMethod);

        confirmation.setTimeOfPayment(getTimeOfPayment(order.getOrderedAt()));
        confirmation.setCertifiedDocuments(collateCertifiedDocuments(item));
    }

    public List<CertifiedDocument> collateCertifiedDocuments(Item item) {
        CertifiedCopyItemOptions itemOptions = (CertifiedCopyItemOptions) item.getItemOptions();
        List<FilingHistoryDocument> filingHistoryDocuments = itemOptions.getFilingHistoryDocuments();
        List<ItemCosts> itemCosts = item.getItemCosts();

        List<CertifiedDocument> certifiedDocuments = new ArrayList<>();
        IntStream.range(0, filingHistoryDocuments.size()).forEach(i -> {
            CertifiedDocument certifiedDocument = new CertifiedDocument();
            certifiedDocument.setDateFiled(reformatDateFiled(filingHistoryDocuments.get(i).getFilingHistoryDate()));
            certifiedDocument.setType(filingHistoryDocuments.get(i).getFilingHistoryType());
            certifiedDocument.setDescription(
                    filingHistoryDescriptionProviderService.mapFilingHistoryDescription(
                            filingHistoryDocuments.get(i).getFilingHistoryDescription(),
                            filingHistoryDocuments.get(i).getFilingHistoryDescriptionValues()));
            certifiedDocument.setFee(itemCosts.get(i).getCalculatedCost());
            certifiedDocuments.add(certifiedDocument);
        });

        return certifiedDocuments;
    }

    /**
     * Reformats a date filed string such as "2009-08-23" as "23 Aug 2009".
     * @param dateFiled the date filed as reported from the filing history
     * @return the same date rendered for display purposes
     */
    @Named("reformatDateFiled")
    default String reformatDateFiled(final String dateFiled) {
        final LocalDate parsedDate = LocalDate.parse(dateFiled);
        return parsedDate.format(DATE_FILED_FORMATTER);
    }
}
