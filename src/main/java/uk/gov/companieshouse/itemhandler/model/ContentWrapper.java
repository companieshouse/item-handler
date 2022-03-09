package uk.gov.companieshouse.itemhandler.model;

import java.util.Objects;

public class ContentWrapper<T> {
    private final T content;

    public ContentWrapper(T content) {
        this.content = content;
    }

    public T getContent() {
        return content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContentWrapper<?> content1 = (ContentWrapper<?>) o;
        return Objects.equals(content, content1.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }
}
