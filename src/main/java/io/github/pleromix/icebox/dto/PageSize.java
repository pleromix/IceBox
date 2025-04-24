package io.github.pleromix.icebox.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.util.Objects;

@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class PageSize {

    private String name;
    private PDRectangle rectangle;

    @Override
    public String toString() {
        if (Objects.nonNull(rectangle)) {
            return String.format("%s: (%.2f × %.2f inches)", name, rectangle.getWidth() / 72, rectangle.getHeight() / 72);
        } else {
            return name;
        }
    }
}
