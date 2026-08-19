package uz.tune.mentourBiz.rest.service.exerciseAnswers;


import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.answers.ResVocabPreviewAnswers;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.answers.ResWritingPreviewAnswers;

import java.util.List;
import java.util.UUID;


public interface StudentAnswersService {
    List<ResWritingPreviewAnswers> getWritingTaskForUnitPreview(UUID unitUuid);
    List<ResVocabPreviewAnswers> getVocabTasksForUnitPreview(UUID unitUuid);
}
