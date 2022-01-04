package uk.gov.companieshouse.itemhandler.kafka;

import static org.junit.jupiter.api.Assertions.assertNull;

import email.email_send;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageDeserialiserTest {
    @Test
    void deserializeReturnsNullIfMessageCannotBeDeserialised() {
        assertNull(new MessageDeserialiser<>(email_send.class).deserialize("email-send",
                "Test data".getBytes()));
    }

    //TODO: positive test for successful deserialisation
}