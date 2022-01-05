package uk.gov.companieshouse.itemhandler.converter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.Assert.assertEquals;

@ExtendWith(SpringExtension.class)
class EnumValueConverterTest {

    enum COLOUR {
        RED,
        YELLOW,
        BABY_BLUE
    }

    @Test
    void successfullyConvertsEnumsToKebabAndLowerCase() {
        assertEquals("baby-blue", EnumValueNameConverter.convertEnumValueNameToJson(COLOUR.BABY_BLUE));
        assertEquals("red", EnumValueNameConverter.convertEnumValueNameToJson(COLOUR.RED));
        assertEquals("yellow", EnumValueNameConverter.convertEnumValueNameToJson(COLOUR.YELLOW));
    }

    @Test
    void successfullyConvertsKebabAndLowerCaseToEnum() {
        assertEquals(COLOUR.BABY_BLUE.toString(), EnumValueNameConverter.convertEnumValueJsonToName("baby-blue"));
        assertEquals(COLOUR.RED.toString(), EnumValueNameConverter.convertEnumValueJsonToName("red"));
        assertEquals(COLOUR.YELLOW.toString(), EnumValueNameConverter.convertEnumValueJsonToName("yellow"));
    }


}
