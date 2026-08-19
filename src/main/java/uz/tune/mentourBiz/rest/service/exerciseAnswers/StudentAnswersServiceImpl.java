package uz.tune.mentourBiz.rest.service.exerciseAnswers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularyQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.postVocabulary.VocabularyAnswer;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.enums.ExerciseType;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.answers.ResVocabPreviewAnswers;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.answers.ResWritingPreviewAnswers;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.unit.UnitRepository;
import uz.tune.mentourBiz.rest.repository.unit.VocabularyAnswerRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseQuestionRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.vocabulary.VocabularyQuestionRepository;
import uz.tune.mentourBiz.rest.service.user.UserService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

;

@Service
@RequiredArgsConstructor
public class StudentAnswersServiceImpl implements StudentAnswersService {


    private final UnitRepository unitRepository;
    private final ExerciseQuestionRepository exerciseQuestionRepository;
    private final VocabularyQuestionRepository vocabularyQuestionRepository;
    private final VocabularyAnswerRepository vocabularyAnswerRepository;
    private final StudentRepo studentRepo;
    private final UserService userService;

    @Override
    public List<ResWritingPreviewAnswers> getWritingTaskForUnitPreview(UUID unitUuid) {
        unitRepository.findByUuid(unitUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.UNIT_NOT_FOUND.getKey()));

        List<ExerciseQuestion> exerciseQuestion = exerciseQuestionRepository.
                findAllByExerciseTask_UuidAndType(unitUuid, ExerciseType.WRITING);

        return exerciseQuestion.stream().map(e -> new ResWritingPreviewAnswers(e.getUuid())).toList();
    }

    @Override
    public List<ResVocabPreviewAnswers> getVocabTasksForUnitPreview(UUID unitUuid) {
        Student student = studentRepo.findByUser(userService.getCurrentUser())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        List<VocabularyQuestion> questions = vocabularyQuestionRepository.findAllByVocabularySet_Unit_Uuid(unitUuid);

        List<VocabularyAnswer> answers = vocabularyAnswerRepository
                .findAllByStudentAndVocabularyQuestionInAndVocabularySet(student, questions, null);

        Map<UUID, VocabularyAnswer> answerMap = answers.stream()
                .filter(a -> a.getVocabularyQuestion() != null)
                .collect(Collectors.toMap(
                        a -> a.getVocabularyQuestion().getUuid(),
                        a -> a,
                        (existing, replacement) -> existing
                ));

        // 5. Build the response list
        return questions.stream()
                .map(q -> new ResVocabPreviewAnswers(q, answerMap.get(q.getUuid())))
                .toList();
    }


}
