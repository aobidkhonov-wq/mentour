package uz.tune.mentourBiz.rest.repository.writing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.writing.WritingSubmission;
import uz.tune.mentourBiz.rest.enums.ExerciseSubmissionStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WritingSubmissionRepository extends BaseRepository<WritingSubmission> {

    Optional<WritingSubmission> findByStudentUuidAndExerciseQuestionUuid(UUID studentUuid, UUID exerciseQuestionUuid);

    Optional<WritingSubmission> findByUuid(UUID uuid);

    @Query("SELECT AVG(ws.score) FROM WritingSubmission ws " +
            "JOIN ws.exerciseQuestion q " +
            "JOIN q.exerciseTask t " +
            "WHERE ws.student.uuid = :studentUuid " +
            "AND t.uuid = :taskUuid")
    Double getAverageWritingScoreByStudentAndTask(@Param("studentUuid") UUID studentUuid, @Param("taskUuid") UUID taskUuid);

    List<WritingSubmission> findAllByExerciseQuestion(ExerciseQuestion question);

    @Query("SELECT ws FROM WritingSubmission ws " +
            "JOIN ws.student s JOIN s.user u " +
            "WHERE (COALESCE(:schoolUuids, NULL) IS NULL OR s.school.uuid IN :schoolUuids) " +
            "AND (:status IS NULL OR ws.status = :status) " +
            "AND (:studentUuid IS NULL OR s.uuid = :studentUuid) " +
            "AND (:teacherUuid IS NULL OR s.id IN (" +
            "    SELECT e.student.id FROM Enrollment e " +
            "    WHERE e.group.teacher.user.uuid = :teacherUuid " +
            "    AND e.status = 'ONGOING')) " +
            "AND (:groupUuid IS NULL OR EXISTS (" +
            "    SELECT 1 FROM Enrollment e2 " +
            "    WHERE e2.student = s " +
            "    AND e2.group.uuid = :groupUuid " +
            "    AND e2.status = 'ONGOING'))")
    Page<WritingSubmission> findAllFilteredMulti(
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("teacherUuid") UUID teacherUuid,
            @Param("status") ExerciseSubmissionStatus status,
            @Param("studentUuid") UUID studentUuid,
            @Param("groupUuid") UUID groupUuid,
            Pageable pageable);

}
