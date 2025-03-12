package uk.gov.companieshouse.itemhandler.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class EnumValueConverterTest {

    enum COLOUR {
        RED,
        YELLOW,
        BABY_BLUE
    }

    @Test
    void successfullyConvertsEnumsToKebabAndLowerCase() {
        Assertions.assertEquals("baby-blue",
                EnumValueNameConverter.convertEnumValueNameToJson(COLOUR.BABY_BLUE));
        Assertions.assertEquals("red",
                EnumValueNameConverter.convertEnumValueNameToJson(COLOUR.RED));
        Assertions.assertEquals("yellow",
                EnumValueNameConverter.convertEnumValueNameToJson(COLOUR.YELLOW));
    }

    @Test
    void successfullyConvertsKebabAndLowerCaseToEnum() {
        Assertions.assertEquals(COLOUR.BABY_BLUE.toString(),
                EnumValueNameConverter.convertEnumValueJsonToName("baby-blue"));
        Assertions.assertEquals(COLOUR.RED.toString(),
                EnumValueNameConverter.convertEnumValueJsonToName("red"));
        Assertions.assertEquals(COLOUR.YELLOW.toString(),
                EnumValueNameConverter.convertEnumValueJsonToName("yellow"));
    }


}
