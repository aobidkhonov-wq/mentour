package uz.tune.mentourBiz.rest.admin.edit;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.rest.admin.req.upd.*;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.SchoolBook;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseTask;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularyQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularySet;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularyWord;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.QuestionContent;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.repository.school.SchoolBookRepository;
import uz.tune.mentourBiz.rest.repository.unit.UnitRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseQuestionRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseTaskRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.VocabularyWordRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.vocabulary.VocabularyQuestionRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.vocabulary.VocabularySetRepository;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminContentServiceEdit {

    private final SchoolBookRepository bookRepo;
    private final UnitRepository unitRepo;
    private final ExerciseTaskRepository taskRepo;
    private final ExerciseQuestionRepository questionRepo;
    private final VocabularyWordRepository vocabularyWordRepo;
    private final VocabularySetRepository vocabSetRepo;
    private final VocabularyQuestionRepository vocabQuestionRepo;
    private final ObjectMapper objectMapper;


    @Transactional
    public ResponseMessage updateBook(UUID uuid, ReqUpdateBook req) {
        SchoolBook book = bookRepo.findByUuid(uuid).orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_BOOK_NOT_FOUND.getKey()));
        if (req.getName() != null) book.setName(req.getName());
        if (req.getStatus() != null) book.setStatus(req.getStatus());
        if (req.getIsGlobal() != null) book.setGlobal(req.getIsGlobal());
        bookRepo.save(book);
        return new ResponseMessage("Book updated");
    }

    @Transactional
    public ResponseMessage updateUnit(UUID uuid, ReqUpdateUnit req) {
        Unit unit = unitRepo.findByUuid(uuid).orElseThrow(() -> new EntityNotFoundException(MessageKey.UNIT_NOT_FOUND.getKey()));
        if (req.getType() != null) unit.setUnitType(req.getType());
        if (req.getTitle() != null) unit.setTitle(req.getTitle());
        if (req.getTopic() != null) unit.setTopic(req.getTopic());
        if (req.getSortOrder() != null) unit.setSortOrder(req.getSortOrder());
        if (req.getStatus() != null) unit.setStatus(req.getStatus());

        unitRepo.save(unit);
        return new ResponseMessage("Unit updated");
    }

    @Transactional
    public ResponseMessage updateTask(UUID uuid, ReqUpdateTask req) {
        ExerciseTask task = taskRepo.findByUuid(uuid).orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_TASK_NOT_FOUND.getKey()));
        if (req.getTitle() != null) task.setTitle(req.getTitle());
        if (req.getTopic() != null) task.setTopic(req.getTopic());
        if (req.getSortOrder() != null) task.setSortOrder(req.getSortOrder());
        if (req.getStatus() != null) task.setStatus(req.getStatus());
        taskRepo.save(task);
        return new ResponseMessage("Task updated");
    }

    @Transactional
    public ResponseMessage updateQuestion(UUID uuid, ReqUpdateQuestion req) {
        ExerciseQuestion q = questionRepo.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_QUESTION_NOT_FOUND.getKey()));

        if (req.getInstruction() != null) q.setInstruction(req.getInstruction());
        if (req.getCoinReward() != null) q.setCoinReward(req.getCoinReward());
        if (req.getScoreReward() != null) q.setScoreReward(req.getScoreReward());

        if (req.getContent() != null) {
//            q.setContent(req.getContent());
//            System.out.println("11111111 1111 Updating question content for question UUID: " + uuid);
//            q.getContent().setAttachmentUrl(req.getContent().getAttachmentUrl());

            String contentJson = null;
            try {
                contentJson = objectMapper.writeValueAsString(req.getContent());
            } catch (JsonProcessingException e) {
                //
            }
            questionRepo.updateContent(uuid, contentJson);
            if (req.getContent().getInstruction() != null) {
                q.setInstruction(req.getContent().getInstruction());
            }
        }

        if (req.getAnswerKey() != null) {
            q.setAnswerKey(req.getAnswerKey());
        }
        questionRepo.save(q);

        return new ResponseMessage("Question updated successfully");
    }
    private static QuestionContent getQuestionContent(ReqUpdateQuestion req, ExerciseQuestion q) {
        QuestionContent content = q.getContent();
        if (req.getContent().getQuestionContent() != null) content.setQuestionContent(req.getContent().getQuestionContent());
        if (req.getContent().getAttachmentUrl() != null) content.setAttachmentUrl(req.getContent().getAttachmentUrl());
        if (req.getContent().getInstruction() != null) content.setInstruction(req.getContent().getInstruction());
        if (req.getContent().getType() != null) content.setType(req.getContent().getType());
        if (req.getContent().getExample() != null) content.setExample(req.getContent().getExample());
        return content;
    }

    @Transactional
    public ResponseMessage updateVocabWord(UUID wordUuid, ReqUpdateVocabWord req) {
        VocabularyWord word = vocabularyWordRepo.findByUuid(wordUuid).orElseThrow(() -> new EntityNotFoundException(MessageKey.WORD_NOT_FOUND.getKey()));
        if (req.getWord() != null) word.setWord(req.getWord());
        if (req.getTranslationUz() != null) word.setTranslationUz(req.getTranslationUz());
        if (req.getTranslationRu() != null) word.setTranslationRu(req.getTranslationRu());
        if (req.getDefinition() != null) word.setDefinition(req.getDefinition());
        if (req.getAudioUrl() != null) word.setAudioUrl(req.getAudioUrl());
        if (req.getAttachmentUrl() != null) word.setAttachmentUrl(req.getAttachmentUrl());
        if(req.getExampleSentence() != null) word.setExampleSentence(req.getExampleSentence());
        if(req.getTranscription() != null) word.setTranscription(req.getTranscription());
        if(req.getPartOfSpeech() != null) word.setPartOfSpeech(req.getPartOfSpeech());
        vocabularyWordRepo.save(word);
        return new ResponseMessage("Vocabulary word updated");
    }

    @Transactional
    public ResponseMessage updateVocabQuestion(UUID questionUuid, ReqUpdateQuestion req) {
        VocabularyQuestion vq = vocabQuestionRepo.findByUuid(questionUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.WORD_NOT_LINKED.getKey()));

        if (req.getCoinReward() != null) vq.setCoinReward(req.getCoinReward());
        vocabQuestionRepo.save(vq);
        return new ResponseMessage("Vocabulary link rewards updated");
    }

    @Transactional
    public ResponseMessage updateVocabSet(UUID setUuid, ReqUpdateVocabSet req) {
        VocabularySet set = vocabSetRepo.findByUuid(setUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.VOCAB_SET_NOT_FOUND.getKey()));
        if (req.getTitle() != null) set.setTitle(req.getTitle());
        if (req.getSortOrder() != null) set.setSortOrder(req.getSortOrder());
        if (req.getStatus() != null) set.setStatus(req.getStatus());
        vocabSetRepo.save(set);
        return new ResponseMessage("Vocabulary Set updated");
    }

    @Transactional
    public ResponseMessage unlinkWordFromSet(UUID questionUuid) {
        vocabQuestionRepo.deleteByUuid(questionUuid);
        return new ResponseMessage("Word removed from set (Word entity still exists in dictionary)");
    }
}
