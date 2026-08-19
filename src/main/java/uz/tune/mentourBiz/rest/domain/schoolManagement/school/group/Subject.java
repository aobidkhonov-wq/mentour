package uz.tune.mentourBiz.rest.domain.schoolManagement.school.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;

@Table(name = "subjects")
@Entity
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Subject extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "is_global", nullable = false)
    private Boolean isGlobal = Boolean.FALSE;

}
