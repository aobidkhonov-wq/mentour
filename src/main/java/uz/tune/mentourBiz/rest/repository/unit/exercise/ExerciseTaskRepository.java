package uz.tune.mentourBiz.rest.repository.unit.exercise;

import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseTask;
import uz.tune.mentourBiz.rest.enums.ExerciseTaskStatus;
import uz.tune.mentourBiz.rest.enums.LessonSectionType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExerciseTaskRepository extends BaseRepository<ExerciseTask> {
    List<ExerciseTask> findAllByUnit_UuidAndStatus(UUID unitUuid, ExerciseTaskStatus status);
    Optional<ExerciseTask> findByUuid(UUID uuid);
    List<ExerciseTask> findAllByUnit_UuidAndSectionTypeOrderBySortOrderAsc(UUID unitUuid, LessonSectionType type);
    List<ExerciseTask> findAllByUnit_UuidAndSectionType(UUID unitUuid, LessonSectionType type);
    List<ExerciseTask> findAllByUnit_UuidAndSectionTypeAndStatus(UUID unitUuid, LessonSectionType type, ExerciseTaskStatus status);
    List<ExerciseTask> findAllByUnit_UuidOrderBySortOrderAsc(UUID unitUuid);
}