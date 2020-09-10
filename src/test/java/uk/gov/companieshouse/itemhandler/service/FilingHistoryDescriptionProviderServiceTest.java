package uk.gov.companieshouse.itemhandler.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;
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


    static {
        DESCRIPTION_VALUES = new HashMap<>();
        DESCRIPTION_VALUES.put("appointment_date", "2010-02-12");
        DESCRIPTION_VALUES.put("officer_name", "Thomas David Wheare");

        DESCRIPTION_VALUES_LEGACY = new HashMap<>();
        DESCRIPTION_VALUES_LEGACY.put("description", "This is the description");
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
    @DisplayName("Returns null when filing history description file not found")
    void mapFilingHistoryDescriptionFileNotFoundReturnsNull() {
        final FilingHistoryDescriptionProviderService provider = new FilingHistoryDescriptionProviderService(new File("notfound.yaml"));
        assertThat(provider.mapFilingHistoryDescription(DESCRIPTION_KEY, DESCRIPTION_VALUES), is(nullValue()));
    }

}
