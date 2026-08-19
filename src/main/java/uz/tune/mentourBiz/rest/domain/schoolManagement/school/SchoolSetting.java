package uz.tune.mentourBiz.rest.domain.schoolManagement.school;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;

@Entity(name = "school_settings")
@Getter
@Setter
public class SchoolSetting extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "school_id", nullable = false, unique = true)
    private School school;

    @Column(name = "max_coin")
    private Long maxCoin;
}