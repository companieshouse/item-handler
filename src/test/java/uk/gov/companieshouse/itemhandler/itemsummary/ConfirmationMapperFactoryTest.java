package uk.gov.companieshouse.itemhandler.itemsummary;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfirmationMapperFactoryTest {

    @Mock
    private CertificateConfirmationMapper certificateConfirmationMapper;

    @Mock
    private CertifiedCopyConfirmationMapper certifiedCopyConfirmationMapper;

    @InjectMocks
    private ConfirmationMapperFactory confirmationMapperFactory;

    @Mock
    private ItemGroup itemGroup;

    @Test
    @DisplayName("Factory returns CertificateConfirmationMapper")
    void testFactoryReturnsCertificateConfirmationMapper() {
        // when
        OrderConfirmationMapper<CertificateEmailData> actual = confirmationMapperFactory.getCertificateMapper();

        //then
        assertEquals(certificateConfirmationMapper, actual);
    }

    @Test
    @DisplayName("Factory returns CertifiedCopyConfirmationMapper")
    void testFactoryReturnsCertifiedCopyConfirmationMapper() {
        // when
        OrderConfirmationMapper<CertifiedCopyEmailData> actual = confirmationMapperFactory.getCertifiedCopyMapper();

        //then
        assertEquals(certifiedCopyConfirmationMapper, actual);
    }
}