package uz.tune.mentourBiz.rest.admin.createContent.createContent;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.SchoolBook;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseTask;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularyQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularySet;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularyWord;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.repository.school.SchoolBookRepository;
import uz.tune.mentourBiz.rest.repository.unit.UnitRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseQuestionRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseTaskRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.VocabularyWordRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.vocabulary.VocabularyQuestionRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.vocabulary.VocabularySetRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminBookDuplicationService {

    private final SchoolBookRepository bookRepo;
    private final UnitRepository unitRepo;
    private final ExerciseTaskRepository taskRepo;
    private final ExerciseQuestionRepository questionRepo;
    private final VocabularySetRepository vocabSetRepo;
    private final VocabularyQuestionRepository vocabQuestionRepo;
    private final VocabularyWordRepository wordRepo;

    @Transactional
    public ResponseMessage duplicateBook(UUID originalBookUuid) {

        SchoolBook originalBook = bookRepo.findByUuid(originalBookUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_BOOK_NOT_FOUND.getKey()));

        SchoolBook newBook = new SchoolBook();
        newBook.setUuid(UUID.randomUUID());
        newBook.setName(originalBook.getName() + " (Cloned)");
        newBook.setLevel(originalBook.getLevel());
        newBook.setStatus(originalBook.getStatus());
        newBook.setGlobal(false);
        newBook.setSchool(originalBook.getSchool());
        newBook.setCreatedBy(originalBook.getCreatedBy());
        bookRepo.save(newBook);

        List<Unit> originalUnits = unitRepo.findAllBySchoolBookUuidOrderBySortOrderAsc(originalBookUuid);
        for (Unit originalUnit : originalUnits) {
            duplicateUnitRecursive(originalUnit, newBook);
        }

        return new ResponseMessage("duplication complete. New Book UUID: " + newBook.getUuid());
    }

    private void duplicateUnitRecursive(Unit originalUnit, SchoolBook newBook) {
        Unit newUnit = new Unit();
        newUnit.setUuid(UUID.randomUUID());
        newUnit.setSchoolBook(newBook);
        newUnit.setTitle(originalUnit.getTitle());
        newUnit.setTopic(originalUnit.getTopic());
        newUnit.setSortOrder(originalUnit.getSortOrder());
        newUnit.setStatus(originalUnit.getStatus());
        unitRepo.save(newUnit);

        List<ExerciseTask> tasks = taskRepo.findAllByUnit_UuidOrderBySortOrderAsc(originalUnit.getUuid());
        for (ExerciseTask originalTask : tasks) {
            duplicateTaskAndQuestions(originalTask, newUnit);
        }

        List<VocabularySet> vocabSets = vocabSetRepo.findAllByUnit_Uuid(originalUnit.getUuid());
        for (VocabularySet originalSet : vocabSets) {
            duplicateVocabStructure(originalSet, newUnit);
        }
    }

    private void duplicateTaskAndQuestions(ExerciseTask originalTask, Unit newUnit) {
        ExerciseTask newTask = new ExerciseTask();
        newTask.setUuid(UUID.randomUUID());
        newTask.setUnit(newUnit);
        newTask.setTitle(originalTask.getTitle());
        newTask.setTopic(originalTask.getTopic());
        newTask.setSortOrder(originalTask.getSortOrder());
        newTask.setStatus(originalTask.getStatus());
        newTask.setSectionType(originalTask.getSectionType());
        newTask.setSubType(originalTask.getSubType());
        taskRepo.save(newTask);

        List<ExerciseQuestion> questions = questionRepo.findAllByExerciseTask_Uuid(originalTask.getUuid());
        for (ExerciseQuestion originalQ : questions) {
            ExerciseQuestion newQ = new ExerciseQuestion();
            newQ.setUuid(UUID.randomUUID());
            newQ.setType(originalQ.getType());
            newQ.setInstruction(originalQ.getInstruction());
            newQ.setCoinReward(originalQ.getCoinReward());
            newQ.setScoreReward(originalQ.getScoreReward());
            newQ.setContent(originalQ.getContent());
            newQ.setAnswerKey(originalQ.getAnswerKey());

            newQ.setExerciseTask(new ArrayList<>(List.of(newTask)));
            questionRepo.save(newQ);
        }
    }

    private void duplicateVocabStructure(VocabularySet originalSet, Unit newUnit) {
        VocabularySet newSet = new VocabularySet();
        newSet.setUuid(UUID.randomUUID());
        newSet.setUnit(newUnit);
        newSet.setTitle(originalSet.getTitle());
        newSet.setSortOrder(originalSet.getSortOrder());
        newSet.setStatus(originalSet.getStatus());
        vocabSetRepo.save(newSet);

        List<VocabularyQuestion> vocabQs = vocabQuestionRepo.findAllByVocabularySet_Uuid(originalSet.getUuid());
        for (VocabularyQuestion originalVq : vocabQs) {
            VocabularyWord originalWord = originalVq.getVocabularyWord();
            VocabularyWord newWord = new VocabularyWord();
            newWord.setUuid(UUID.randomUUID());
            newWord.setWord(originalWord.getWord());
            newWord.setTranslationUz(originalWord.getTranslationUz());
            newWord.setTranslationRu(originalWord.getTranslationRu());
            newWord.setDefinition(originalWord.getDefinition());
            newWord.setAudioUrl(originalWord.getAudioUrl());
            newWord.setAttachmentUrl(originalWord.getAttachmentUrl());
            newWord.setExampleSentence(originalWord.getExampleSentence());
            newWord.setTranscription(originalWord.getTranscription());
            newWord.setPartOfSpeech(originalWord.getPartOfSpeech());
            wordRepo.save(newWord);

            VocabularyQuestion newVq = new VocabularyQuestion();
            newVq.setUuid(UUID.randomUUID());
            newVq.setVocabularySet(newSet);
            newVq.setVocabularyWord(newWord);
            newVq.setInstruction(originalVq.getInstruction());
            newVq.setCoinReward(originalVq.getCoinReward());
            newVq.setScoreReward(originalVq.getScoreReward());
            vocabQuestionRepo.save(newVq);
        }
    }
}