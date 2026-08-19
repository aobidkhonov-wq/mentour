package uz.tune.mentourBiz.rest.repository.unit.exercise;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseQuestion;
import uz.tune.mentourBiz.rest.enums.ExerciseType;
import uz.tune.mentourBiz.rest.enums.LessonSectionType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExerciseQuestionRepository extends BaseRepository<ExerciseQuestion> {
    Optional<ExerciseQuestion> findByUuid(UUID uuid);
    Optional<ExerciseQuestion> findByUuidAndExerciseTaskUuid(UUID uuid, UUID exerciseTaskId);

    List<ExerciseQuestion> findAllByExerciseTask_UuidAndType(UUID unitUuid, ExerciseType type);
    List<ExerciseQuestion> findAllByExerciseTask_Uuid(UUID taskUuid);

    // Idempotent bulk delete: a concurrent/duplicate request deleting 0 rows does not raise
    // an optimistic-lock (StaleObjectState) error, unlike the entity-managed delete.
    @Modifying
    @Transactional
    @Query("DELETE FROM ExerciseQuestion q WHERE q.id = :id")
    void deleteByIdBulk(@Param("id") Long id);


    @Modifying
    @Transactional
    @Query(value = "UPDATE questions SET content = CAST(:content AS jsonb) WHERE uuid = :uuid",
            nativeQuery = true)
    void updateContent(@Param("uuid") UUID uuid, @Param("content") String content);

    @Query("SELECT COALESCE(SUM(q.scoreReward), 0) FROM ExerciseQuestion q " +
            "JOIN q.exerciseTask t " +
            "WHERE t.unit.uuid = :unitUuid " +
            "AND t.sectionType = :type " +
            "AND t.status = 'ACTIVE'")
    Long sumPossibleScoreByUnitAndSectionType(@Param("unitUuid") UUID unitUuid, @Param("type") LessonSectionType type);

    @Query("SELECT COALESCE(SUM(q.scoreReward), 0) FROM ExerciseQuestion q " +
            "JOIN q.exerciseTask t " +
            "WHERE t.unit.uuid = :unitUuid " +
            "AND t.unit.status = 'ACTIVE' " +
            "AND t.status = 'ACTIVE'")
    Long sumPossibleScoreByUnit(@Param("unitUuid") UUID unitUuid);
}