package uz.tune.mentourBiz.rest.payload.res;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.config.GlobalVar;
import uz.tune.mentourBiz.rest.domain.Notification;
import uz.tune.mentourBiz.rest.enums.Lang;
import uz.tune.mentourBiz.rest.enums.NotificationTargetType;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
public class ResNotification {
    private String title;
    private String content;
    private Instant createdAt;
    private NotificationTargetType targetType;
    private boolean isRead;
    private String createdBy;
    private ResAttachment image;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public ResNotification(Notification n) {
        String resolvedTitle = n.getTitle();
        String resolvedContent = n.getContent();

        if (n.getLocalizations() != null) {
            try {
                Lang lang = GlobalVar.getLang();
                if (lang == null) lang = Lang.RUS;
                Map<String, Map<String, String>> locs = MAPPER.readValue(
                        n.getLocalizations(),
                        new TypeReference<>() {}
                );
                Map<String, String> lc = locs.get(lang.name());
                if (lc != null) {
                    resolvedTitle = lc.get("title");
                    resolvedContent = lc.get("content");
                }
            } catch (Exception ignored) {}
        }

        this.title = resolvedTitle;
        this.content = resolvedContent;
        this.createdAt = n.getCreatedAt();
        this.isRead = n.isRead();
        this.targetType = n.getTargetType();
        this.createdBy = n.getCreatedBy();
        if (n.getAttachment() != null) {
            this.image = new ResAttachment(n.getAttachment());
        }
    }
}