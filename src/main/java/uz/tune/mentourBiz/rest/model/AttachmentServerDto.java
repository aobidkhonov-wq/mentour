package uz.tune.mentourBiz.rest.model;

import java.io.InputStream;

public record AttachmentServerDto(
        String contentType,
        String name,
        String originalName,
        Long size,
        String fullPath,
        InputStream inputStream,
        String directory
) {
}
