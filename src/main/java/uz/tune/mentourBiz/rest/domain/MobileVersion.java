package uz.tune.mentourBiz.rest.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.enums.AppType;
import uz.tune.mentourBiz.rest.enums.Platform;

@Table(name = "mobile_version")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MobileVersion extends BaseEntity {

    @Column(name = "last_version")
    private String version;

    @Column(name = "platform")
    @Enumerated(EnumType.STRING)
    private Platform platform;

    // Qaysi mobil ilova uchun: STUDENT (asosiy ilova) yoki PARENT (Parent Edu).
    // ColumnDefault mavjud yozuvlar ustun qo'shilganda STUDENT bo'lib qolishi uchun.
    @Column(name = "app_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'STUDENT'")
    private AppType appType = AppType.STUDENT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;
}