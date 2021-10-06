package uk.gov.companieshouse.itemhandler.config;

import java.util.Objects;

public class FeatureOptions {
    private boolean llpCertificateOrdersEnabled;
    private boolean lpCertificateOrdersEnabled;

    public FeatureOptions(boolean llpCertificateOrdersEnabled, boolean lpCertificateOrdersEnabled) {
        this.llpCertificateOrdersEnabled = llpCertificateOrdersEnabled;
        this.lpCertificateOrdersEnabled = lpCertificateOrdersEnabled;
    }

    public boolean isLlpCertificateOrdersEnabled() {
        return llpCertificateOrdersEnabled;
    }

    public boolean isLpCertificateOrdersEnabled() {
        return lpCertificateOrdersEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FeatureOptions that = (FeatureOptions) o;
        return llpCertificateOrdersEnabled == that.llpCertificateOrdersEnabled && lpCertificateOrdersEnabled == that.lpCertificateOrdersEnabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(llpCertificateOrdersEnabled, lpCertificateOrdersEnabled);
    }
}
