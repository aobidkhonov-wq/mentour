package uz.tune.mentourBiz.rest.domain.userManagement.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;

@Table(name = "mentors")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Mentor extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "about_me")
    private String aboutMe;
}
