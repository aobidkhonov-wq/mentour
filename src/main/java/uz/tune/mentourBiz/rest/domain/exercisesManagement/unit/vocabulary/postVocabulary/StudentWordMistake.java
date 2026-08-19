package uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.postVocabulary;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularyWord;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;


import java.util.UUID;

@Table(name = "word_mistakes")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentWordMistake extends BaseEntity {
    @Column(name = "uuid" , unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "mistake_count")
    private Integer mistakeCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="vocabulary_word_id")
    private VocabularyWord vocabularyWord;
}
