package uk.gov.companieshouse.itemhandler.itemsummary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

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
}
