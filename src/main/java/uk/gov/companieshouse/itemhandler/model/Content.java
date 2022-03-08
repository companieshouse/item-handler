package uk.gov.companieshouse.itemhandler.model;

import java.util.Objects;

public class Content<T> {
    private T content;

    public Content(T content) {
        this.content = content;
    }

    public T getContent() {
        return content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Content<?> content1 = (Content<?>) o;
        return Objects.equals(content, content1.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }
}
