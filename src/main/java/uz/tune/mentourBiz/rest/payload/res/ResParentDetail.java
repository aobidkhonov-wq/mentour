package uz.tune.mentourBiz.rest.payload.res;

import lombok.Data;
import uz.tune.mentourBiz.rest.domain.StudentParentContact;
import uz.tune.mentourBiz.rest.enums.Lang;

import java.util.UUID;

@Data
public class ResParentDetail {
    private UUID uuid;
    private String name;
    private String telegramNickname;
    private String phoneNumber;
    private Lang language;
    private boolean isActive;
    private String telegramChatId;

    public ResParentDetail(StudentParentContact contact) {
        this.uuid = contact.getUuid();
        this.name = contact.getParentName();
        this.telegramNickname = contact.getTelegramNickname();
        this.phoneNumber = contact.getPhoneNumber();
        this.language = contact.getLanguage();
        this.isActive = contact.isActive();
        this.telegramChatId = contact.getTelegramChatId();
    }
}