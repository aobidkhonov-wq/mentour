package uz.tune.mentourBiz.rest.admin.createContent.createContent;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.rest.admin.req.create.adminCreate.*;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.SchoolBook;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseTask;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularyQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularySet;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularyWord;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Level;
import uz.tune.mentourBiz.rest.enums.UnitType;
import uz.tune.mentourBiz.rest.enums.VocabularySetStatus;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.repository.group.LevelRepository;
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
public class AdminContentServiceCreateImpl {

    private final SchoolBookRepository bookRepo;
    private final UnitRepository unitRepo;
    private final ExerciseTaskRepository taskRepo;
    private final ExerciseQuestionRepository questionRepo;
    private final VocabularyWordRepository vocabularyWordRepo;
    private final VocabularySetRepository vocabSetRepo;
    private final VocabularyQuestionRepository vocabQuestionRepo;
    private final LevelRepository levelRepo;
    private final VocabularyQuestionRepository vocabularyQuestionRepository;
    private final SchoolBookRepository schoolBookRepository;

    @Transactional
    public ResponseMessage createBook(List<ReqCreateBook> reqSb) {
        List<SchoolBook> schoolBooks = new ArrayList<>();
        reqSb.forEach(req -> {
            Level level = levelRepo.findByUuid(req.getLevelUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.LEVEL_NOT_FOUND.getKey()));

            SchoolBook book = new SchoolBook();
            book.setUuid(UUID.randomUUID());
            book.setName(req.getName());
            book.setLevel(level);
            book.setStatus(req.getStatus());
            book.setGlobal(req.isGlobal());
            schoolBooks.add(book);
        });
        schoolBookRepository.saveAll(schoolBooks);
        return new ResponseMessage("Book created: ");
    }

    @Transactional
    public ResponseMessage createUnit(List<ReqCreateUnit> reqCU) {
        List<Unit> units = new ArrayList<>();
        reqCU.forEach(req -> {
            SchoolBook book = bookRepo.findByUuid(req.getBookUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_BOOK_NOT_FOUND.getKey()));

            Unit unit = new Unit();
            unit.setSchoolBook(book);
            unit.setTitle(req.getTitle());
            unit.setTopic(req.getTopic());
            unit.setSortOrder(req.getSortOrder());
            unit.setStatus(req.getStatus());
            unit.setUnitType(req.getType() != null ? req.getType() : UnitType.REGULAR);
            units.add(unit);
        });
        unitRepo.saveAll(units);
        return new ResponseMessage("Unit created: ");
    }

    @Transactional
    public ResponseMessage createTask(List<ReqCreateTask> reqCt) {
        List<ExerciseTask> tasks = new ArrayList<>();
        reqCt.forEach(req -> {
            Unit unit = unitRepo.findByUuid(req.getUnitUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.UNIT_NOT_FOUND.getKey()));

            ExerciseTask task = new ExerciseTask();
            task.setUnit(unit);
            task.setTitle(req.getTitle());
            task.setTopic(req.getTopic());
            task.setSortOrder(req.getSortOrder());
            task.setSectionType(req.getSectionType());
            tasks.add(task);
        });
        taskRepo.saveAll(tasks);
        return new ResponseMessage("Task created: ");
    }

    @Transactional
    public ResponseMessage createQuestion(List<ReqCreateQuestion> reqCq) {
        List<ExerciseQuestion> exerciseQuestions = new ArrayList<>();
        reqCq.forEach(req -> {
            ExerciseTask task = taskRepo.findByUuid(req.getTaskUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_TASK_NOT_FOUND.getKey()));

            ExerciseQuestion q = new ExerciseQuestion();
            q.setType(req.getType());
            String instruction = (req.getInstruction() != null)
                    ? req.getInstruction()
                    : (req.getContent() != null ? req.getContent().getInstruction() : null);
            q.setInstruction(instruction);
            q.setCoinReward((req.getCoinReward() != null) ? req.getCoinReward() : 0);
            q.setScoreReward((req.getScoreReward() !=null) ? req.getScoreReward() :0);
            q.setContent(req.getContent());
            q.setAnswerKey(req.getAnswerKey());


            q.setExerciseTask(new ArrayList<>());
            q.getExerciseTask().add(task);
            exerciseQuestions.add(q);

        });
        questionRepo.saveAll(exerciseQuestions);
        return new ResponseMessage("Question created: ");
    }

    @Transactional
    public ResponseMessage createVocabWord(List<ReqCreateVocabWord> reqVw) {
        List<VocabularyWord> vw = new ArrayList<>();
        List<VocabularyQuestion> vocabQs = new ArrayList<>();;
        reqVw.forEach(req -> {
            VocabularySet set = vocabSetRepo.findByUuid(req.getSetUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.VOCAB_SET_NOT_FOUND.getKey()));

            VocabularyWord word = new VocabularyWord();
            word.setWord(req.getWord());
            word.setTranslationUz(req.getTranslationUz());
            word.setTranslationRu(req.getTranslationRu());
            word.setDefinition(req.getDefinition());
            word.setAudioUrl(req.getAudioUrl());
            word.setAttachmentUrl(req.getAttachmentUrl());
            word.setExampleSentence(req.getExampleSentence());
            word.setTranscription(req.getTranscription());
            word.setPartOfSpeech(req.getPartOfSpeech());
            word.setTranslationKaa(req.getTranslationKaa());
            word.setTranslationTjk(req.getTranslationTjk());
            word.setTranslationKrg(req.getTranslationKrg());
            vw.add(word);


            VocabularyQuestion vq = new VocabularyQuestion();
            vq.setVocabularySet(set);
            vq.setVocabularyWord(word);
            vq.setInstruction("Translate the word");
            vq.setCoinReward(2);
            vq.setScoreReward(2);
            vocabQs.add(vq);
        });
        vocabularyWordRepo.saveAll(vw);
        vocabularyQuestionRepository.saveAll(vocabQs);

        return new ResponseMessage("Vocabulary word created and linked:");
    }

    @Transactional
    public ResponseMessage createVocabSet(List<ReqCreateVocabSet> reqVs) {
        List<VocabularySet> vocabularySets = new ArrayList<>();
        reqVs.forEach(req -> {
            Unit unit = unitRepo.findByUuid(req.getUnitUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.UNIT_NOT_FOUND.getKey()));

            VocabularySet set = new VocabularySet();
            set.setUnit(unit);
            set.setTitle(req.getTitle());
            set.setSortOrder(req.getSortOrder());

            set.setStatus(req.getStatus() != null ? req.getStatus() : VocabularySetStatus.ACTIVE);

            vocabularySets.add(set);
        });

        vocabSetRepo.saveAll(vocabularySets);
        return new ResponseMessage("Vocab Set created: ");
    }

    @Transactional
    public ResponseMessage linkWordToSet(List<ReqLinkWordToSet> reqWl) {
        List<VocabularyQuestion> vocabularyQuestions = new ArrayList<>();
        reqWl.forEach(req -> {
            VocabularySet set = vocabSetRepo.findByUuid(req.getSetUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.VOCAB_SET_NOT_FOUND.getKey()));
            VocabularyWord word = vocabularyWordRepo.findByUuid(req.getWordUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.WORD_NOT_FOUND.getKey()));

            VocabularyQuestion vq = new VocabularyQuestion();
            vq.setVocabularySet(set);
            vq.setVocabularyWord(word);
            vq.setCoinReward(2);
            vq.setScoreReward(2);
            vq.setInstruction("Translate the word");
            vocabularyQuestions.add(vq);
        });

        vocabQuestionRepo.saveAll(vocabularyQuestions);
        return new ResponseMessage("Word linked to set successfully");
    }
}