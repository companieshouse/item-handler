package uk.gov.companieshouse.itemhandler.itemsummary;

import java.util.Objects;

public class CertifiedCopySummary {
    private String itemNumber;
    private String filingHistoryId;
    private String dateFiled;
    private String type;
    private String description;
    private String companyNumber;
    private String fee;

    public CertifiedCopySummary() {
    }

    public CertifiedCopySummary(String itemNumber, String filingHistoryId, String dateFiled, String type,
                                String description, String companyNumber, String fee) {
        this.itemNumber = itemNumber;
        this.filingHistoryId = filingHistoryId;
        this.dateFiled = dateFiled;
        this.type = type;
        this.description = description;
        this.companyNumber = companyNumber;
        this.fee = fee;
    }

    public String getItemNumber() {
        return itemNumber;
    }

    public void setItemNumber(String itemNumber) {
        this.itemNumber = itemNumber;
    }

    public String getFilingHistoryId() {
        return filingHistoryId;
    }

    public void setFilingHistoryId(String filingHistoryId) {
        this.filingHistoryId = filingHistoryId;
    }

    public String getDateFiled() {
        return dateFiled;
    }

    public void setDateFiled(String dateFiled) {
        this.dateFiled = dateFiled;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCompanyNumber() {
        return companyNumber;
    }

    public void setCompanyNumber(String companyNumber) {
        this.companyNumber = companyNumber;
    }

    public String getFee() {
        return fee;
    }

    public void setFee(String fee) {
        this.fee = fee;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String itemNumber;
        private String filingHistoryId;
        private String dateFiled;
        private String type;
        private String description;
        private String companyNumber;
        private String fee;

        public Builder withItemNumber(String itemNumber) {
            this.itemNumber = itemNumber;
            return this;
        }

        public Builder withFilingHistoryId(String filingHistoryId) {
            this.filingHistoryId = filingHistoryId;
            return this;
        }

        public Builder withDateFiled(String dateFiled) {
            this.dateFiled = dateFiled;
            return this;
        }

        public Builder withType(String type) {
            this.type = type;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withCompanyNumber(String companyNumber) {
            this.companyNumber = companyNumber;
            return this;
        }

        public Builder withFee(String fee) {
            this.fee = fee;
            return this;
        }

        public CertifiedCopySummary build() {
            return new CertifiedCopySummary(itemNumber, filingHistoryId, dateFiled, type, description, companyNumber, fee);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CertifiedCopySummary that = (CertifiedCopySummary) o;
        return Objects.equals(itemNumber, that.itemNumber) &&
                Objects.equals(filingHistoryId, that.filingHistoryId) &&
                Objects.equals(dateFiled, that.dateFiled) &&
                Objects.equals(type, that.type) &&
                Objects.equals(description, that.description) &&
                Objects.equals(companyNumber, that.companyNumber) &&
                Objects.equals(fee, that.fee);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemNumber, filingHistoryId, dateFiled, type, description, companyNumber, fee);
    }
}
