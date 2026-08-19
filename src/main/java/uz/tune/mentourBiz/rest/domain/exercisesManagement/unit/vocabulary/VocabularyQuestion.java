package uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;

import java.util.UUID;

@Table(name = "vocabulary_questions")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyQuestion extends BaseEntity {
    @Column(name = "uuid" , unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "coin_reward")
    private Integer coinReward;

    @Column(name = "score_reward")
    private Integer scoreReward;

    @Column(name = "instruction", columnDefinition = "TEXT")
    private String instruction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocabulary_set_id")
    private VocabularySet vocabularySet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocabulary_words_id")
    private VocabularyWord vocabularyWord;

}
