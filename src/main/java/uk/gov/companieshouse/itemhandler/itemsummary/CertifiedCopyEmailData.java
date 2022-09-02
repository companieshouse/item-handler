package uk.gov.companieshouse.itemhandler.itemsummary;

import uk.gov.companieshouse.itemhandler.model.DeliveryDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CertifiedCopyEmailData extends EmailData {

    private final List<CertifiedCopySummary> certifiedCopies = new ArrayList<>();

    public void add(CertifiedCopySummary certifiedCopy) {
        certifiedCopies.add(certifiedCopy);
    }

    public void addAll(List<CertifiedCopySummary> certifiedCopies) {
        this.certifiedCopies.addAll(certifiedCopies);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String to;
        private String subject;
        private String orderReference;
        private DeliveryDetails deliveryDetails;
        private PaymentDetails paymentDetails;
        private final List<CertifiedCopySummary> certifiedCopies = new ArrayList<>();

        public Builder withTo(String to) {
            this.to = to;
            return this;
        }

        public Builder withSubject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder withOrderReference(String reference) {
            this.orderReference = reference;
            return this;
        }

        public Builder withDeliveryDetails(DeliveryDetails deliveryDetails) {
            this.deliveryDetails = deliveryDetails;
            return this;
        }

        public Builder withPaymentDetails(PaymentDetails paymentDetails) {
            this.paymentDetails = paymentDetails;
            return this;
        }

        public Builder addCertifiedCopy(CertifiedCopySummary certifiedCopy) {
            this.certifiedCopies.add(certifiedCopy);
            return this;
        }

        public CertifiedCopyEmailData build() {
            CertifiedCopyEmailData certifiedCopyEmailData = new CertifiedCopyEmailData();
            certifiedCopyEmailData.setTo(to);
            certifiedCopyEmailData.setSubject(subject);
            certifiedCopyEmailData.setOrderReference(orderReference);
            certifiedCopyEmailData.setDeliveryDetails(deliveryDetails);
            certifiedCopyEmailData.setPaymentDetails(paymentDetails);
            certifiedCopyEmailData.addAll(certifiedCopies);
            return certifiedCopyEmailData;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CertifiedCopyEmailData)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        CertifiedCopyEmailData that = (CertifiedCopyEmailData) o;
        return Objects.equals(certifiedCopies, that.certifiedCopies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), certifiedCopies);
    }

    @Override
    public String toString() {
        return "CertifiedCopyEmailData{" +
                "certifiedCopies=" + certifiedCopies +
                '}';
    }
}
