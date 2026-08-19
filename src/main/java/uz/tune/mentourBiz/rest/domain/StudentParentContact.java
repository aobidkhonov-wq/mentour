package uz.tune.mentourBiz.rest.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.enums.Lang;

import java.util.UUID;

@Table(name = "student_parent_contacts")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentParentContact extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "parent_name", nullable = false)
    private String parentName;

    @Column(name = "telegram_chat_id")
    private String telegramChatId;

    @Column(name = "telegram_nickname")
    private String telegramNickname;

    @Column(name = "use_telegram")
    private boolean useTelegram = true;

    @Column(name = "use_whatsapp")
    private boolean useWhatsapp = false;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "is_active")
    private boolean isActive = true;
    @Enumerated(EnumType.STRING)
    @Column(name = "language")
    private Lang language = Lang.UZB;
}