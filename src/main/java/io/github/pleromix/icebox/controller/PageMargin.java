package io.github.pleromix.icebox.controller;

import java.util.Objects;

public enum PageMargin {
    None,
    Small("\u00BD inch"),
    Medium("1 inch"),
    Large("\u00B3\u2044\u2082 inch");

    private String size;

    PageMargin() {
    }

    PageMargin(String size) {
        this.size = size;
    }

    @Override
    public String toString() {
        if (Objects.nonNull(size)) {
            return String.format("%s (%s)", name(), size);
        } else {
            return name();
        }
    }
}
