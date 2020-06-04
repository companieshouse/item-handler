package uk.gov.companieshouse.itemhandler.util;

import java.time.LocalDateTime;

/**
 * Represents objects bearing created at and updated at timestamp properties.
 */
public interface TimestampedEntity {

   LocalDateTime getCreatedAt();

   LocalDateTime getUpdatedAt();

}
