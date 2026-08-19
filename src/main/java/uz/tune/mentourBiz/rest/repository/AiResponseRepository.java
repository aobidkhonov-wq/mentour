package uz.tune.mentourBiz.rest.repository;

import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.speaking.AiResponse;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.speaking.SpeakingSubmission;

import java.util.Optional;

public interface AiResponseRepository extends BaseRepository<AiResponse> {
    Optional<AiResponse> findBySpeakingSubmission(SpeakingSubmission speakingSubmission);
}
