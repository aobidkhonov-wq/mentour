package uz.tune.mentourBiz.rest.payload.res.attachment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import uz.tune.mentourBiz.rest.domain.Attachment;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResAttachment {

    private UUID uuid;

    private String contentType;

    private String path;

    private String name;

    public ResAttachment(Attachment attachment) {
        this.uuid = attachment.getUuid();
        this.contentType = attachment.getContentType();

        String baseUrl = CoreUtils.getBaseFileUrl();
        String rawPath = attachment.getName();

        String cleanPath = rawPath.replace("/var/www/mentour/", "");

        if (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }

        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        this.path = baseUrl + cleanPath;
        this.name = cleanPath;
    }


}
