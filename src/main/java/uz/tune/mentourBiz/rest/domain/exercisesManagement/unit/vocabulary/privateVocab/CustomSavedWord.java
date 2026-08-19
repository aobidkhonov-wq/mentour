package uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.privateVocab;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;


import java.util.UUID;

@Table(name = "custom_saved_words")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomSavedWord extends BaseEntity {
    @Column(name = "uuid" , unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "custom_word")
    private String customWord;

    @Column(name = "translation")
    private String translation;

    @Column(name = "context")
    private String context;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;
}
