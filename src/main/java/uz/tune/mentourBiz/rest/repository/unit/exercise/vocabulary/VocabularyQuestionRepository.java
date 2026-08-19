package uz.tune.mentourBiz.rest.repository.unit.exercise.vocabulary;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularyQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularyWord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VocabularyQuestionRepository extends BaseRepository<VocabularyQuestion> {

    List<VocabularyQuestion> findAllByVocabularySet_Uuid(UUID vocabularySetUuid);

    List<VocabularyQuestion> findAllByVocabularySet_Unit_Uuid(UUID vocabularySetUnitUuid);
    List<VocabularyQuestion> findAllByVocabularyWord(VocabularyWord word);

    @Query("SELECT COALESCE(SUM(vq.scoreReward), 0) FROM VocabularyQuestion vq " +
            "WHERE vq.vocabularySet.unit.uuid = :unitUuid " +
            "AND vq.vocabularySet.status = 'ACTIVE'")
    Long sumPossibleVocabScoreByUnit(@Param("unitUuid") UUID unitUuid);

    Optional<VocabularyQuestion> findByUuid(UUID uuid);
    void deleteByUuid(UUID uuid);
    @Query("""
    SELECT CASE WHEN COUNT(vq) > 0 THEN true ELSE false END 
    FROM VocabularyQuestion vq 
    JOIN vq.vocabularySet.unit.schoolBook b
    WHERE vq.vocabularyWord.uuid = :wordUuid 
    AND (
        b.school.uuid = :schoolUuid 
        OR (b.isGlobal = true AND EXISTS (
            SELECT 1 FROM School s 
            JOIN s.allowedBooks ab 
            WHERE s.uuid = :schoolUuid AND ab.id = b.id
        ))
    )
""")
    boolean existsByWordUuidAndSchoolUuid(@Param("wordUuid") UUID wordUuid, @Param("schoolUuid") UUID schoolUuid);

    Optional<VocabularyQuestion> findByVocabularyWordAndVocabularySetUuid(VocabularyWord word, UUID vocabularySetUuid);

}
