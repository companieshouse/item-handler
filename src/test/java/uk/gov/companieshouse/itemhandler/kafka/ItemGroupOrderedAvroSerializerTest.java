package uk.gov.companieshouse.itemhandler.kafka;

import org.apache.kafka.common.errors.SerializationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.itemgroupordered.ItemGroupOrdered;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Unit tests {@link ItemGroupOrderedAvroSerializer}.
 */
@ExtendWith(MockitoExtension.class)
class ItemGroupOrderedAvroSerializerTest {

    @Mock
    private ItemGroupOrdered message;

    @Test
    @DisplayName("serialize() propagates error as expected SerializationException")
    void testSerializePropagatesException() {

        // Given
        when(message.getSchema()).thenReturn(ItemGroupOrdered.SCHEMA$);
        final ItemGroupOrderedAvroSerializer serializerUnderTest = new ItemGroupOrderedAvroSerializer();

        // When and then
        final SerializationException exception = assertThrows(SerializationException.class,
                () -> serializerUnderTest.serialize("item-group-ordered", message));
        assertThat(exception.getMessage(), is("Error when serializing ItemGroupOrdered to byte[], error: " +
                "null of string of uk.gov.companieshouse.itemgroupordered.ItemGroupOrdered"));
    }

}