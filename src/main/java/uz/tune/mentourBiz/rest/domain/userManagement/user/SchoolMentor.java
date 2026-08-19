package uz.tune.mentourBiz.rest.domain.userManagement.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;

import java.util.UUID;

@Table(name = "school_mentors")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SchoolMentor extends BaseEntity {

    @Column(name = "uuid")
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id")
    private Mentor mentor;

    @Column(name = "contract_hours")
    private Integer contractHours;

}
