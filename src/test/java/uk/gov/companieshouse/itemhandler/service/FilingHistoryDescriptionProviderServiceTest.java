package uk.gov.companieshouse.itemhandler.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

public class FilingHistoryDescriptionProviderServiceTest {
    private static final String DESCRIPTION_KEY = "appoint-person-director-company-with-name-date";
    private static final Map<String, Object> DESCRIPTION_VALUES;
    private static final String EXPECTED_DESCRIPTION = "Appointment of Thomas David Wheare as a director on 12 February 2010";

    private static final String DESCRIPTION_KEY_LEGACY = "legacy";
    private static final Map<String, Object> DESCRIPTION_VALUES_LEGACY;
    private static final String EXPECTED_DESCRIPTION_LEGACY = "This is the description";

    private static final String DESCRIPTION_KEY_ARRAY = "capital-allotment-shares";
    private static final Map<String, Object> DESCRIPTION_VALUES_ARRAY;
    private static final String EXPECTED_DESCRIPTION_ARRAY = "Statement of capital following an allotment of shares on 10 November 2019";


    static {
        DESCRIPTION_VALUES = new HashMap<>();
        DESCRIPTION_VALUES.put("appointment_date", "2010-02-12");
        DESCRIPTION_VALUES.put("officer_name", "Thomas David Wheare");

        DESCRIPTION_VALUES_LEGACY = new HashMap<>();
        DESCRIPTION_VALUES_LEGACY.put("description", "This is the description");

        DESCRIPTION_VALUES_ARRAY = new HashMap<>();
        DESCRIPTION_VALUES_ARRAY.put("date", "2019-11-10");
        Map<String, String> capital = new HashMap<>();
        capital.put("figure", "34,253,377");
        capital.put("currency", "GBP");
        DESCRIPTION_VALUES_ARRAY.put("capital", capital);
    }

    @Test
    @DisplayName("Replace the values in the description with the values in the descriptionValues, format date and remove any asterisks")
    void mapFilingHistoryDescriptionGetsDescriptionAndReplacesVariables() {
        final FilingHistoryDescriptionProviderService provider = new FilingHistoryDescriptionProviderService();
        assertThat(provider.mapFilingHistoryDescription(DESCRIPTION_KEY, DESCRIPTION_VALUES), is(EXPECTED_DESCRIPTION));
    }

    @Test
    @DisplayName("Return the description in the descriptionValues if it is present")
    void mapFilingHistoryDescriptionGetsDescriptionAndReturnsDescriptionInDescriptionValue() {
        final FilingHistoryDescriptionProviderService provider = new FilingHistoryDescriptionProviderService();
        assertThat(provider.mapFilingHistoryDescription(DESCRIPTION_KEY_LEGACY, DESCRIPTION_VALUES_LEGACY), is(EXPECTED_DESCRIPTION_LEGACY));
    }

    @Test
    @DisplayName("Ignore description value if it is not a String")
    void mapFilingHistoryDescriptionIgnoresDescriptionValuesThatAreNotStrings() {
        final FilingHistoryDescriptionProviderService provider = new FilingHistoryDescriptionProviderService();
        assertThat(provider.mapFilingHistoryDescription(DESCRIPTION_KEY_ARRAY, DESCRIPTION_VALUES_ARRAY), is(EXPECTED_DESCRIPTION_ARRAY));
    }

    @Test
    @DisplayName("Returns null when filing history description file not found")
    void mapFilingHistoryDescriptionFileNotFoundReturnsNull() {
        final FilingHistoryDescriptionProviderService provider = new FilingHistoryDescriptionProviderService(new File("notfound.yaml"));
        assertThat(provider.mapFilingHistoryDescription(DESCRIPTION_KEY, DESCRIPTION_VALUES), is(nullValue()));
    }

}
