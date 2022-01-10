package uk.gov.companieshouse.itemhandler.kafka;

public final class PartitionOffset {
    private ThreadLocal<Long> offset = new ThreadLocal<>();

    public void setOffset(Long offset) {
        this.offset.set(offset);
    }

    public Long getOffset() {
        return this.offset.get();
    }

    public void reset() {
        this.offset = new ThreadLocal<>();
    }
}
