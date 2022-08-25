package uk.gov.companieshouse.itemhandler.model;

import java.util.ArrayList;
import java.util.List;

public class CertificateEmailData extends EmailData {
    private final List<CertificateSummary> certificates = new ArrayList<>();

    public void add(CertificateSummary certificate) {
        certificates.add(certificate);
    }

    public void addAll(List<CertificateSummary> certificates) {
        this.certificates.addAll(certificates);
    }
}
