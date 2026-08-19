
package uz.tune.mentourBiz.rest.payload.req;

import lombok.Data;
import uz.tune.mentourBiz.rest.enums.Lang;

import java.util.List;
import java.util.UUID;

@Data
public class ReqAddParents {
    private UUID studentUuid;
    private List<ParentDetail> parents;

    @Data
    public static class ParentDetail {
        private String name;
        private String telegramNickname;
        private String phoneNumber;
        private boolean useTelegram;
        private boolean useWhatsapp;
        private Lang language;
    }
}