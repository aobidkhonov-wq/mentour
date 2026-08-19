package uz.tune.mentourBiz.rest.payload.studentRes.res.bookmark;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.BookmarkedQuestion;
import uz.tune.mentourBiz.rest.enums.BookmarkStatus;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResExerciseQuestion;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class ResBookmark {
    private UUID bookmarkUuid;
    private String comment;
    private BookmarkStatus status;
    private Instant createdAt;
    private ResExerciseQuestion question;

    public ResBookmark(BookmarkedQuestion bookmarkedQuestion) {
        this.bookmarkUuid = bookmarkedQuestion.getUuid();
        this.comment = bookmarkedQuestion.getComment();
        this.status = bookmarkedQuestion.getStatus();
        this.createdAt = bookmarkedQuestion.getCreatedAt();
        if (bookmarkedQuestion.getQuestion() != null) {
            this.question = new ResExerciseQuestion(bookmarkedQuestion.getQuestion());
        }
    }
}