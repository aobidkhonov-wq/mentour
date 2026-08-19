package uz.tune.mentourBiz.rest.repository.unit.exercise;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.BookmarkedQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseQuestion;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.enums.BookmarkStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookmarkedQuestionRepository extends BaseRepository<BookmarkedQuestion> {

    Optional<BookmarkedQuestion> findByUuid(UUID uuid);
    BookmarkedQuestion findByStudentAndQuestion(Student student, ExerciseQuestion question);
    List<BookmarkedQuestion> findAllByQuestion_UuidAndGroup_Uuid(UUID qsUuid, UUID grUuid);

    @Query("SELECT DISTINCT(bq.question.id)  FROM BookmarkedQuestion bq WHERE bq.group.id =: groupUuid AND bq.status= 'OPEN'")
    List<BookmarkedQuestion> findAllByGroup_UuidAndStatusOrderByCreatedAtDesc(UUID groupUuid, BookmarkStatus status);

    // Bookmarks have a NOT NULL FK to the question, so they must be removed before the
    // question is deleted. Bulk delete keeps it idempotent under concurrent requests.
    @Modifying
    @Transactional
    @Query("DELETE FROM BookmarkedQuestion b WHERE b.question.id = :questionId")
    void deleteAllByQuestionId(@Param("questionId") Long questionId);

}