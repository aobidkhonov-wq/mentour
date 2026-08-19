package uz.tune.mentourBiz.rest.model;

import uz.tune.mentourBiz.rest.domain.Attachment;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.util.UUID;

public record ResAttachmentModel(UUID id, String contentType, String path, String name) {

    public ResAttachmentModel(Attachment attachment) {
        this(attachment.getUuid(),
                attachment.getContentType(),
                CoreUtils.getBaseFileUrl() + attachment.getName(),
                attachment.getName());
    }
}
