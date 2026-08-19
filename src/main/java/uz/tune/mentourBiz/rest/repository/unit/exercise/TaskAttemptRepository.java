package uz.tune.mentourBiz.rest.repository.unit.exercise;

import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.postExercise.TaskAttempt;

import java.util.Optional;
import java.util.UUID;

public interface TaskAttemptRepository extends BaseRepository<TaskAttempt> {

    Optional<TaskAttempt> findByStudent_UuidAndTask_Uuid(UUID studentUuid, UUID taskUuid);

    Optional<TaskAttempt> findByStudent_User_UuidAndTask_Uuid(UUID studentUserUuid, UUID taskUuid);
}
