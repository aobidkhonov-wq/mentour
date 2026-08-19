package uz.tune.mentourBiz.rest.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.enums.Lang;

@Entity
@Table(name = "telegram_bot_users")
@Getter
@Setter
@NoArgsConstructor

@AllArgsConstructor
public class TelegramBotUser extends BaseEntity {
    @Column(unique = true)
    private String chatId;

    @Column(unique = true)
    private String phoneNumber;

    private String username;

    @Enumerated(EnumType.STRING)
    private Lang language;
}