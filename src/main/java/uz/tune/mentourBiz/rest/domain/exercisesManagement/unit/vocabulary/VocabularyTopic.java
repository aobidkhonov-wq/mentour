package uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;

import java.util.List;
import java.util.UUID;

@Table(name = "vocabulary_topics")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyTopic extends BaseEntity {

    @Column(name = "uuid" , unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "name")
    private String name;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "word_topic_links",
            joinColumns = @JoinColumn(name = "topic_id"),
            inverseJoinColumns = @JoinColumn(name = "word_id")
    )
    private List<VocabularyWord> words;

}
