package uz.tune.mentourBiz.rest.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.speaking.SpeakingSubmission;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseQuestion;
import uz.tune.mentourBiz.rest.enums.ExerciseSubmissionStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpeakingSubmissionRepository extends BaseRepository<SpeakingSubmission> {
    Optional<SpeakingSubmission> findByUuid(UUID uuid);

    Optional<SpeakingSubmission> findByStudent_User_UuidAndExerciseQuestion_Uuid(UUID studentUuid, UUID questionUuid);

    List<SpeakingSubmission> findAllByExerciseQuestion(ExerciseQuestion question);

    @Query("""
        SELECT ss FROM SpeakingSubmission ss 
        JOIN ss.student s 
        JOIN s.user u 
        WHERE (COALESCE(:schoolUuids, NULL) IS NULL OR s.school.uuid IN :schoolUuids)
        AND ss.pronunciationData IS NULL
        AND (:status IS NULL OR ss.status = :status)
        AND (:groupUuid IS NULL OR EXISTS (
            SELECT 1 FROM Enrollment e 
            WHERE e.student = s 
            AND e.group.uuid = :groupUuid 
            AND e.status = 'ONGOING'
        ))
        AND (:teacherUuid IS NULL OR EXISTS (
            SELECT 1 FROM Enrollment e2 
            WHERE e2.student = s 
            AND e2.group.teacher.user.uuid = :teacherUuid 
            AND e2.status = 'ONGOING'
        ))
    """)
    Page<SpeakingSubmission> findAllFilteredMulti(
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("status") ExerciseSubmissionStatus status,
            @Param("groupUuid") UUID groupUuid,
            @Param("teacherUuid") UUID teacherUuid,
            Pageable pageable);

}
