package uz.tune.mentourBiz.rest.payload.req;

import lombok.Data;
import java.util.UUID;

@Data
public class ReqLinkParent {
    private UUID studentUuid;
    private String telegramChatId;
    private String parentName;
    private String language;
}