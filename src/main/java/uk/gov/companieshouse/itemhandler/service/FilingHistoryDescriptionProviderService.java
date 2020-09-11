package uk.gov.companieshouse.itemhandler.service;

import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.itemhandler.util.DateConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.DESCRIPTION_LOG_KEY;

@Service
public class FilingHistoryDescriptionProviderService {

    private static final String FILING_HISTORY_DESCRIPTIONS_FILEPATH = "api-enumerations/filing_history_descriptions.yml";
    private static final String FILING_HISTORY_DESCRIPTION_KEY = "description";
    private static final String LOG_MESSAGE_FILE_KEY = "file";

    private Map<String, String> filingHistoryDescriptions;

    public FilingHistoryDescriptionProviderService() {
        final File filingHistoryDescriptionsFile = new File(FILING_HISTORY_DESCRIPTIONS_FILEPATH);
        filingHistoryDescriptions = loadFilingHistoryDescriptionsFromFile(filingHistoryDescriptionsFile);
    }

    public FilingHistoryDescriptionProviderService(final File filingHistoryDescriptionsFile) {
        filingHistoryDescriptions = loadFilingHistoryDescriptionsFromFile(filingHistoryDescriptionsFile);
    }

    private Map<String, String> loadFilingHistoryDescriptionsFromFile(final File filingHistoryDescriptionsFile) {
        if (!filingHistoryDescriptionsFile.exists()) {
            Map<String, Object> logMap = new HashMap<>();
            logMap.put(LOG_MESSAGE_FILE_KEY, filingHistoryDescriptionsFile.getAbsolutePath());
            LoggingUtils.getLogger().error("Orders descriptions file not found", logMap);
            return null;
        }
        Map<String, String> filingHistoryDescriptionsLocal = null;
        try(final InputStream inputStream = new FileInputStream(filingHistoryDescriptionsFile)) {
            final Yaml yaml = new Yaml();
            final Map<String, Object> filingHistoryDescriptionsRoot = yaml.load(inputStream);
            filingHistoryDescriptionsLocal =
                    (Map<String, String>) filingHistoryDescriptionsRoot.get(FILING_HISTORY_DESCRIPTION_KEY);
            if (filingHistoryDescriptionsLocal == null) {
                Map<String, Object> logMap = new HashMap<>();
                logMap.put(DESCRIPTION_LOG_KEY, FILING_HISTORY_DESCRIPTION_KEY);
                LoggingUtils.getLogger().error("Filing History descriptions file not found", logMap);
                return null;
            }
        } catch (IOException ioe) {
            // This is very unlikely to happen here given File.exists() check above,
            // and that it is not likely to encounter an error closing the stream either.
            LoggingUtils.getLogger().error(ioe);
        }
        return filingHistoryDescriptionsLocal;
    }

    private String getFilingHistoryDescriptionWithKey(String descriptionKey) {
        if(descriptionKey == null || descriptionKey.isEmpty()) {
            return descriptionKey;
        } else {
            return filingHistoryDescriptions.get(descriptionKey);
        }
    }

    /**
     * Fetches the specified key from the filing history enumerations and replaces the variables
     * with the ones in the description values
     * @param descriptionKey the key to fetch from the filing history enumerations
     * @param filingHistoryDescriptionValues the map of the description values to replace in the description
     * @return the formatted filing history description
     */
    public String mapFilingHistoryDescription(String descriptionKey, Map<String, Object> filingHistoryDescriptionValues) {
        if(filingHistoryDescriptions == null) {
            return null;
        }
        String description = getFilingHistoryDescriptionWithKey(descriptionKey);
        if(filingHistoryDescriptionValues.containsKey("description")) {
            return (String) filingHistoryDescriptionValues.get("description");
        } else {
            final StringBuilder sb = new StringBuilder(description);
            replaceAll(sb, "*", ""); // remove all asterisks in fetched string
            filingHistoryDescriptionValues.forEach((k,v)-> {
                String value = k.contains("date") ? reformatActionDate((String)v) : (String) v;
                replaceAll(sb, "{"+k+"}", value);
            });
            return sb.toString();
        }
    }

    /**
     * Reformats an action date string such as "2009-08-23" as "23 August 2009".
     * @param actionDate the action date as reported from the filing history
     * @return the same date rendered for display purposes
     */
    public static String reformatActionDate(final String actionDate) {
        final LocalDate parsedDate = LocalDate.parse(actionDate);
        return parsedDate.format(DateConstants.FILING_HISTORY_DATE_DESCRIPTION_FORMATTER);
    }

    /**
     * Replaces a specified String in a StringBuilder
     * @param builder the StringBuilder
     * @param from the String you want to replace
     * @param to the String you want it to be replaced by
     * @return the same StringBuilder with the values replaces
     */
    public static void replaceAll(StringBuilder builder, String from, String to) {
        int index = builder.indexOf(from);
        while (index != -1) {
            builder.replace(index, index + from.length(), to);
            index += to.length();
            index = builder.indexOf(from, index);
        }
    }

}
