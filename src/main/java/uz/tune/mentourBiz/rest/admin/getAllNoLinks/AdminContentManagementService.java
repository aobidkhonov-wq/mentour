package uz.tune.mentourBiz.rest.admin.getAllNoLinks;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.rest.admin.res.ResContentHeader;
import uz.tune.mentourBiz.rest.enums.LessonSectionType;
import uz.tune.mentourBiz.rest.payload.res.ResBooks;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResUnit;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResExerciseQuestion;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResVocabLearnWord;
import uz.tune.mentourBiz.rest.repository.school.SchoolBookRepository;
import uz.tune.mentourBiz.rest.repository.unit.UnitRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseQuestionRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseTaskRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.VocabularyWordRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.vocabulary.VocabularySetRepository;

@Service
@RequiredArgsConstructor
public class AdminContentManagementService {

    private final SchoolBookRepository bookRepo;
    private final UnitRepository unitRepo;
    private final ExerciseTaskRepository taskRepo;
    private final ExerciseQuestionRepository questionRepo;
    private final VocabularyWordRepository wordRepo;
    private final VocabularySetRepository setRepo;

    @Transactional(readOnly = true)
    public Page<ResBooks> getAllBooks(Pageable pageable) {
        return bookRepo.findAll(pageable).map(ResBooks::new);
    }

    @Transactional(readOnly = true)
    public Page<ResUnit> getAllUnits(Pageable pageable) {
        return unitRepo.findAll(pageable).map(ResUnit::new);
    }

    @Transactional(readOnly = true)
    public Page<ResContentHeader> getAllTasks(Pageable pageable) {
        return taskRepo.findAll(pageable).map(t ->
                new ResContentHeader(t.getUuid(), t.getTitle(), t.getTopic(),
                        t.getSortOrder(), t.getStatus().name(), t.getSectionType()));
    }

    @Transactional(readOnly = true)
    public Page<ResExerciseQuestion> getAllQuestions(Pageable pageable) {
        return questionRepo.findAll(pageable).map(ResExerciseQuestion::new);
    }

    @Transactional(readOnly = true)
    public Page<ResVocabLearnWord> getAllWords(Pageable pageable) {
        return wordRepo.findAll(pageable).map(ResVocabLearnWord::new);
    }

    @Transactional(readOnly = true)
    public Page<ResContentHeader> getAllVocabSets(Pageable pageable) {
        return setRepo.findAll(pageable).map(s ->
                new ResContentHeader(s.getUuid(), s.getTitle(), s.getTitle(),
                        s.getSortOrder(), s.getStatus().name(), LessonSectionType.VOCABULARY));
    }
}