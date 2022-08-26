package uk.gov.companieshouse.itemhandler.itemsummary;

import uk.gov.companieshouse.itemhandler.model.DeliveryDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CertificateEmailData extends EmailData {
    private final List<CertificateSummary> certificates = new ArrayList<>();

    public void add(CertificateSummary certificate) {
        certificates.add(certificate);
    }

    public void addAll(List<CertificateSummary> certificates) {
        this.certificates.addAll(certificates);
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
        private final List<CertificateSummary> certificates = new ArrayList<>();

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

        public Builder addCertificate(CertificateSummary certificate) {
            this.certificates.add(certificate);
            return this;
        }

        public CertificateEmailData build() {
            CertificateEmailData certificateEmailData = new CertificateEmailData();
            certificateEmailData.setTo(to);
            certificateEmailData.setSubject(subject);
            certificateEmailData.setOrderReference(orderReference);
            certificateEmailData.setDeliveryDetails(deliveryDetails);
            certificateEmailData.setPaymentDetails(paymentDetails);
            certificateEmailData.addAll(certificates);
            return certificateEmailData;
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
        CertificateEmailData that = (CertificateEmailData) o;
        return Objects.equals(certificates, that.certificates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(certificates);
    }
}
