package uz.tune.mentourBiz.rest.domain.exercisesManagement.unit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.SchoolBook;
import uz.tune.mentourBiz.rest.enums.UnitStatus;
import uz.tune.mentourBiz.rest.enums.UnitType;

import java.util.UUID;

@Table(name = "units")
@Entity
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Unit extends BaseEntity {

    @Column(name = "uuid" , unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "topic")
    private String topic;

    @Column(name = "title")
    private String title;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "unit_status")
    @Enumerated(EnumType.STRING)
    private UnitStatus status;

    @ManyToOne
    @JoinColumn(name = "school_book_id")
private SchoolBook schoolBook;

    @Column(name = "unit_type")
    @Enumerated(EnumType.STRING)
    private UnitType unitType = UnitType.REGULAR;

}