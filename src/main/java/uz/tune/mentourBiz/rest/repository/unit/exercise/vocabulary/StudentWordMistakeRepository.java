package uz.tune.mentourBiz.rest.repository.unit.exercise.vocabulary;

import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularyWord;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.postVocabulary.StudentWordMistake;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;

import java.util.Optional;

@Repository
public interface StudentWordMistakeRepository extends BaseRepository<StudentWordMistake> {
    Optional<StudentWordMistake> findByStudentAndVocabularyWord(Student student, VocabularyWord vocabularyWord);
}