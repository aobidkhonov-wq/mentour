package uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.enums.VocabularySetStatus;

import java.util.UUID;

@Table(name = "vocabulary_sets")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VocabularySet extends BaseEntity {
    @Column(name = "uuid" , unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "title")
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private VocabularySetStatus status = VocabularySetStatus.ACTIVE;

    @Formula("(SELECT count(q.id) FROM vocabulary_questions q WHERE q.vocabulary_set_id = id)")
    private int questionCount;
}
