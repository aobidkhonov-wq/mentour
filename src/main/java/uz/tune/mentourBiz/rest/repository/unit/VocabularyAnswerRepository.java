package uz.tune.mentourBiz.rest.repository.unit;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularyQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularySet;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.postVocabulary.VocabularyAnswer;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VocabularyAnswerRepository extends BaseRepository<VocabularyAnswer> {
    Optional<VocabularyAnswer> findByStudentAndVocabularyQuestion(Student student, VocabularyQuestion question);
    List<VocabularyAnswer> findAllByStudentAndVocabularyQuestionInAndVocabularySet(Student student,
                                                                                   List<VocabularyQuestion> questions,
                                                                                   VocabularySet vocabularySet);

    Optional<VocabularyAnswer> findByUuid(UUID uuid);

    @Query("SELECT COALESCE(SUM(va.score), 0) FROM VocabularyAnswer va " +
            "WHERE va.student.id = :studentId " +
            "AND va.vocabularySet.unit.uuid = :unitUuid " +
            "AND va.vocabularySet.status = 'ACTIVE'")
    Long sumTotalEarnedVocabScoreByStudentAndUnit(@Param("studentId") Long studentId, @Param("unitUuid") UUID unitUuid);

    @Query("SELECT va.student.id, va.vocabularySet.unit.uuid, COALESCE(SUM(va.score), 0) " +
            "FROM VocabularyAnswer va " +
            "WHERE va.student.id IN :studentIds " +
            "AND va.vocabularySet.unit.uuid IN :unitUuids " +
            "AND va.vocabularySet.status = 'ACTIVE' " +
            "GROUP BY va.student.id, va.vocabularySet.unit.uuid")
    List<Object[]> sumEarnedVocabGroupedByStudentsAndUnits(@Param("studentIds") List<Long> studentIds,
                                                           @Param("unitUuids") List<UUID> unitUuids);

}
