package uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;

import java.util.UUID;

@Table(name = "vocabulary_words")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyWord extends BaseEntity {

    @Column(name = "uuid" , unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "word")
    private String word;

    @Column(name = "translation_uz")
    private String translationUz;

    @Column(name = "translation_ru")
    private String translationRu;

    @Column(name = "translation_tjk")
    private String translationTjk;

    @Column(name = "translation_kaa")
    private String translationKaa;

    @Column(name = "translation_krg")
    private String translationKrg;

    @Column(name = "definition")
    private String definition;

    @Column(name = "part_of_speech")
    private String partOfSpeech;

    @Column(name = "transcription")
    private String transcription;

    @Column(name = "example_sentence")
    private String exampleSentence;

    @Column(name = "audio_url")
    private String audioUrl;

    @Column(name = "attachment_url")
    private String attachmentUrl;

}
