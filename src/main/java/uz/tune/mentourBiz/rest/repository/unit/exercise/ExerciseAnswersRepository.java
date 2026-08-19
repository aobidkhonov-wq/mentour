package uz.tune.mentourBiz.rest.repository.unit.exercise;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseTask;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.postExercise.ExerciseAnswers;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.enums.LessonSectionType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExerciseAnswersRepository extends BaseRepository<ExerciseAnswers> {

    Optional<ExerciseAnswers> findByStudentAndExerciseQuestionAndExerciseTask(Student student, ExerciseQuestion question, ExerciseTask task);

    long countByStudentAndExerciseQuestionAndExerciseTask(Student student, ExerciseQuestion question, ExerciseTask task);

    Optional<ExerciseAnswers> findBySubmissionId(UUID submissionId);

    @Query("SELECT COALESCE(SUM(ea.score), 0) FROM ExerciseAnswers ea " +
            "WHERE ea.student.id = :studentId " +
            "AND ea.exerciseTask.unit.uuid = :unitUuid " +
            "AND ea.exerciseTask.sectionType = :type " +
            "AND ea.exerciseTask.status = 'ACTIVE'")
    Long sumTotalEarnedScoreByStudentAndUnitAndSectionType(@Param("studentId") Long studentId,
                                                           @Param("unitUuid") UUID unitUuid,
                                                           @Param("type") LessonSectionType type);

    Optional<ExerciseAnswers> findByStudentAndExerciseQuestion(Student student, ExerciseQuestion question);

    @Query("SELECT COALESCE(SUM(ea.score), 0) FROM ExerciseAnswers ea " +
            "WHERE ea.student.id = :studentId " +
            "AND ea.exerciseTask.unit.uuid = :unitUuid " +
            "AND ea.exerciseTask.status = 'ACTIVE'")
    Long sumTotalEarnedScoreByStudentAndUnit(@Param("studentId") Long studentId, @Param("unitUuid") UUID unitUuid);

    @Query("SELECT ea.student.id, ea.exerciseTask.unit.uuid, COALESCE(SUM(ea.score), 0) " +
            "FROM ExerciseAnswers ea " +
            "WHERE ea.student.id IN :studentIds " +
            "AND ea.exerciseTask.unit.uuid IN :unitUuids " +
            "AND ea.exerciseTask.status = 'ACTIVE' " +
            "GROUP BY ea.student.id, ea.exerciseTask.unit.uuid")
    List<Object[]> sumEarnedGroupedByStudentsAndUnits(@Param("studentIds") List<Long> studentIds,
                                                      @Param("unitUuids") List<UUID> unitUuids);

    List<ExerciseAnswers> findAllByStudentAndExerciseQuestionIn(Student student, List<ExerciseQuestion> questions);

    List<ExerciseAnswers> findAllByExerciseQuestion(ExerciseQuestion question);

}   