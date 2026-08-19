package uz.tune.mentourBiz.rest.service.vocabulary;


import uz.tune.mentourBiz.rest.payload.res.exercise.ResVocabSet;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResVocabGradingResult;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResVocabLearnWord;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResVocabQuizWord;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResVocabSetResult;

import java.util.List;
import java.util.UUID;

public interface VocabularyService {
    List<ResVocabSet> getVocabularySetsForUnit(UUID unitId);
    List<ResVocabQuizWord> getQuizWordsForSet(UUID setUuid);
    ResVocabGradingResult gradeVocabulary(UUID wordUuid, String userTranslation,UUID setUuid);
    ResVocabSetResult getVocabularySetResult(UUID setUuid, Boolean flag);
    ResVocabSetResult getVocabResultForStudent(UUID studentUuid, UUID setUuid, Boolean flag);
    List<ResVocabLearnWord> getLearnWordsForSet(UUID setUuid);

}
