package uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.privateVocab;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularyWord;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;

import java.util.UUID;

@Table(name = "saved_words")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SavedWord extends BaseEntity {
    @Column(name = "uuid" , unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocabular_word_id")
    private VocabularyWord vocabularyWord;
}
