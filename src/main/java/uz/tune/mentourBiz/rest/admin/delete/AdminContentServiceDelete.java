package uz.tune.mentourBiz.rest.admin.delete;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.SchoolBook;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseTask;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.postExercise.ExerciseAnswers;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularyQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularySet;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularyWord;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.postVocabulary.VocabularyAnswer;
import uz.tune.mentourBiz.rest.enums.SchoolBookStatus;
import uz.tune.mentourBiz.rest.enums.UnitStatus;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.repository.SpeakingSubmissionRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolBookRepository;
import uz.tune.mentourBiz.rest.repository.unit.UnitRepository;
import uz.tune.mentourBiz.rest.repository.unit.VocabularyAnswerRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.BookmarkedQuestionRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseAnswersRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseQuestionRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseTaskRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.VocabularyWordRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.vocabulary.VocabularyQuestionRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.vocabulary.VocabularySetRepository;
import uz.tune.mentourBiz.rest.repository.writing.WritingSubmissionRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminContentServiceDelete {

    private final SchoolBookRepository bookRepo;
    private final UnitRepository unitRepo;
    private final ExerciseTaskRepository taskRepo;
    private final ExerciseQuestionRepository questionRepo;
    private final VocabularySetRepository vocabSetRepo;
    private final VocabularyQuestionRepository vocabQuestionRepo;
    private final VocabularyWordRepository vocabularyWordRepos;
    private final VocabularyQuestionRepository vocabularyQuestionRepository;
    private final ExerciseAnswersRepository exerciseAnswersRepository;
    private final VocabularyAnswerRepository vocabularyAnswerRepository;
    private final WritingSubmissionRepository writingSubmissionRepository;
    private final SpeakingSubmissionRepository speakingSubmissionRepository;
    private final BookmarkedQuestionRepository bookmarkedQuestionRepository;

    @Transactional
    public ResponseMessage deleteBook(UUID uuid) {
        SchoolBook book = bookRepo.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_BOOK_NOT_FOUND.getKey()));

        book.setStatus(SchoolBookStatus.INACTIVE);
        bookRepo.save(book);
        return new ResponseMessage("Book marked as INACTIVE");
    }

    @Transactional
    public ResponseMessage deleteUnit(UUID uuid) {
        Unit unit = unitRepo.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.UNIT_NOT_FOUND.getKey()));

        unit.setStatus(UnitStatus.INACTIVE);
        unitRepo.save(unit);
        return new ResponseMessage("Unit marked as INACTIVE");
    }

    @Transactional
    public ResponseMessage deleteTask(UUID uuid) {
        ExerciseTask task = taskRepo.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_TASK_NOT_FOUND.getKey()));

        List<ExerciseQuestion> questions = questionRepo.findAllByExerciseTask_Uuid(uuid);
        for (ExerciseQuestion q : questions) {
            q.getExerciseTask().remove(task);
        }
        questionRepo.saveAll(questions);

        taskRepo.delete(task);
        return new ResponseMessage("Task deleted successfully");
    }

    @Transactional
    public ResponseMessage deleteQuestion(UUID uuid) {
        ExerciseQuestion q = questionRepo.findByUuid(uuid).orElse(null);
        if (q == null) {
            // Already removed by a concurrent/duplicate request - treat as success (idempotent).
            return new ResponseMessage("Question already deleted.");
        }

        // Detach historical answers (preserve them). Find by question, not by a null student.
        List<ExerciseAnswers> associatedAnswers = exerciseAnswersRepository.findAllByExerciseQuestion(q);
        for (ExerciseAnswers ans : associatedAnswers) {
            ans.setExerciseQuestion(null);
        }
        exerciseAnswersRepository.saveAll(associatedAnswers);

        writingSubmissionRepository.findAllByExerciseQuestion(q).forEach(ws -> {
            ws.setExerciseQuestion(null);
            writingSubmissionRepository.save(ws);
        });

        speakingSubmissionRepository.findAllByExerciseQuestion(q).forEach(ss -> {
            ss.setExerciseQuestion(null);
            speakingSubmissionRepository.save(ss);
        });

        // Bookmarks have a NOT NULL FK to the question - remove them first.
        bookmarkedQuestionRepository.deleteAllByQuestionId(q.getId());

        // Remove the ManyToMany task links (collection deletes are not existence-checked).
        q.getExerciseTask().clear();
        questionRepo.saveAndFlush(q);

        // Idempotent delete: a racing duplicate request may have already removed the row.
        questionRepo.deleteByIdBulk(q.getId());
        return new ResponseMessage("Question deleted successfully. Student answers were preserved.");
    }

    @Transactional
    public ResponseMessage deleteVocabSet(UUID uuid) {
        VocabularySet set = vocabSetRepo.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.VOCAB_SET_NOT_FOUND.getKey()));

        List<VocabularyQuestion> links = vocabQuestionRepo.findAllByVocabularySet_Uuid(uuid);
        vocabQuestionRepo.deleteAll(links);

        vocabSetRepo.delete(set);
        return new ResponseMessage("Vocab set and its links deleted");
    }

    @Transactional
    public ResponseMessage deleteVocabWord(UUID uuid) {
        VocabularyWord word = vocabularyWordRepos.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.WORD_NOT_FOUND.getKey()));

        List<VocabularyQuestion> questions = vocabularyQuestionRepository.findAllByVocabularyWord(word);

        for (VocabularyQuestion vq : questions) {
            List<VocabularyAnswer> answers = vocabularyAnswerRepository.findAllByStudentAndVocabularyQuestionInAndVocabularySet(null, List.of(vq), null);
            for (VocabularyAnswer va : answers) {
                va.setVocabularyQuestion(null);
            }
            vocabularyAnswerRepository.saveAll(answers);
        }

        vocabularyQuestionRepository.deleteAll(questions);
        vocabularyWordRepos.delete(word);

        return new ResponseMessage("Word deleted from dictionary. Historical answers were preserved.");
    }
}
