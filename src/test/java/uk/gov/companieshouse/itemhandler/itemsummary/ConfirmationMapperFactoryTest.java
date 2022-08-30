package uk.gov.companieshouse.itemhandler.itemsummary;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ConfirmationMapperFactoryTest {

    @Mock
    private CertificateConfirmationMapper certificateConfirmationMapper;

    @InjectMocks
    private ConfirmationMapperFactory confirmationMapperFactory;

    @Mock
    private ItemGroup itemGroup;

    @Test
    @DisplayName("Factory returns certificateConfirmationMapper if item kind is certificate")
    void testFactoryReturnsCertificateConfirmationMapper() {
        // given
        when(itemGroup.getKind()).thenReturn("item#certificate");

        // when
        OrderConfirmationMapper<?> actual = confirmationMapperFactory.getMapper(itemGroup);

        //then
        assertEquals(certificateConfirmationMapper, actual);
    }
    @Test
    @DisplayName("Factory throws IllegalArgumentException for unhandled item kinds")
    void testFactoryThrowsIllegalArgumentException() {
        // given
        when(itemGroup.getKind()).thenReturn("unknown_kind");

        // when
        Executable actual = () -> confirmationMapperFactory.getMapper(itemGroup);

        // then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, actual);
        assertThat(exception.getMessage(), is("Kind [unknown_kind] unhandled"));
    }
}
