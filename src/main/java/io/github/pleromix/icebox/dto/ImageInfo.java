package io.github.pleromix.icebox.dto;

import lombok.Data;
import lombok.NonNull;

import java.io.File;

@Data
public class ImageInfo {
    @NonNull
    private String name;
    @NonNull
    private Long size;
    @NonNull
    private File originalFile;
    @NonNull
    private File thumbnailFile;
    @NonNull
    private Integer page;
    @NonNull
    private Integer width;
    @NonNull
    private Integer height;
}
