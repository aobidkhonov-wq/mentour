package uz.tune.mentourBiz.rest.admin.get;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.rest.admin.res.ResContentHeader;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularySet;
import uz.tune.mentourBiz.rest.enums.LessonSectionType;
import uz.tune.mentourBiz.rest.payload.res.ResBooks;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResUnit;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResAdminQuestions;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResExerciseQuestion;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResVocabLearnWord;
import uz.tune.mentourBiz.rest.repository.school.SchoolBookRepository;
import uz.tune.mentourBiz.rest.repository.unit.UnitRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseQuestionRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseTaskRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.VocabularyWordRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.vocabulary.VocabularyQuestionRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.vocabulary.VocabularySetRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminContentServiceGet {

    private final SchoolBookRepository bookRepo;
    private final UnitRepository unitRepo;
    private final ExerciseTaskRepository taskRepo;
    private final ExerciseQuestionRepository questionRepo;
    private final VocabularySetRepository vocabSetRepo;
    private final VocabularyQuestionRepository vocabQuestionRepo;
    private final VocabularyWordRepository vocabularyWordRepository;
    private final VocabularySetRepository vocabularySetRepository;


    public List<ResBooks> getBooks() {
        return bookRepo.findAll().stream().map(ResBooks::new).toList();
    }


    public List<ResUnit> getUnits(UUID bookUuid) {
        return unitRepo.findAllBySchoolBookUuidOrderBySortOrderAsc(bookUuid)
                .stream()
                .map(ResUnit::new)
                .toList(); // f а
    }

    @Transactional(readOnly = true)
    public List<ResVocabLearnWord> getWordsBySetUnitUuid(UUID unitUuid) {
        Unit unit = unitRepo.findByUuid(unitUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.UNIT_NOT_FOUND.getKey()));

        List<VocabularySet> vocabSet = vocabularySetRepository.findAllByUnit_Uuid(unitUuid);

        return vocabQuestionRepo.findAllByVocabularySet_Unit_Uuid(unitUuid)
                .stream()
                .map(q -> new ResVocabLearnWord(q.getVocabularyWord(), q))
                .toList();
    }

    @Transactional(readOnly = true)
    public ResAdminQuestions getQuestionByUuid(UUID uuid) {
        return questionRepo.findByUuid(uuid)
                .map(ResAdminQuestions::new)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_QUESTION_NOT_FOUND.getKey()));
    }

    @Transactional(readOnly = true)
    public ResVocabLearnWord getVocabWordByUuid(UUID uuid) {
        return vocabularyWordRepository.findByUuid(uuid)
                .map(ResVocabLearnWord::new)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.WORD_NOT_FOUND.getKey()));
    }

    public List<ResContentHeader> getTasks(UUID unitUuid, LessonSectionType type) {
        return taskRepo.findAllByUnit_UuidAndSectionType(unitUuid, type).stream()
                .map(t -> new ResContentHeader(t.getUuid(), t.getTitle(), t.getTopic(), t.getSortOrder(), t.getStatus().name() ,t.getSectionType()))
                .toList();
    }


    @Transactional(readOnly = true)
    public List<ResExerciseQuestion> getQuestions(UUID taskUuid) {
        return questionRepo.findAllByExerciseTask_Uuid(taskUuid).stream()
                .map(ResExerciseQuestion::new)
                .toList();
    }

    public List<ResContentHeader> getVocabSets(UUID unitUuid) {
        return vocabSetRepo.findAllByUnit_Uuid(unitUuid).stream()
                .map(v -> new ResContentHeader(v.getUuid(), v.getTitle(), v.getTitle(), v.getSortOrder(), v.getStatus().name() ,LessonSectionType.VOCABULARY))
                .toList();
    }


        @Transactional(readOnly = true)
    public List<ResVocabLearnWord> getVocabWords(UUID setUuid) {
        return vocabQuestionRepo.findAllByVocabularySet_Uuid(setUuid).stream()
                .map(vq -> new ResVocabLearnWord(vq.getVocabularyWord()))
                .toList();

    }
}