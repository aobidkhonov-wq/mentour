package uz.tune.mentourBiz.rest.domain.crm;

import jakarta.persistence.*;
import lombok.Getter;
import uz.tune.mentourBiz.base.BaseEntity;

import java.util.UUID;

@Table(name = "crm_demo_lessons")
@Entity
@Getter
public class CrmDemoLesson extends BaseEntity {

    @Column(name = "lesson_id")
    private UUID lessonId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private CrmLead lead;

    @Column(name = "status")
    private String status;
}
