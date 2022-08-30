package uk.gov.companieshouse.itemhandler.itemsummary;

public class ConfirmationMapperFactory {

    private final CertificateConfirmationMapper certificateConfirmationMapper;

    public ConfirmationMapperFactory(CertificateConfirmationMapper certificateConfirmationMapper) {
        this.certificateConfirmationMapper = certificateConfirmationMapper;
    }

    public OrderConfirmationMapper<?> getMapper(ItemGroup itemGroup) {
        String kind = itemGroup.getKind();
        if ("item#certificate".equals(kind)) {
            return certificateConfirmationMapper;
        } else {
            throw new IllegalArgumentException(String.format("Kind [%s] unhandled", kind));
        }
    }
}
