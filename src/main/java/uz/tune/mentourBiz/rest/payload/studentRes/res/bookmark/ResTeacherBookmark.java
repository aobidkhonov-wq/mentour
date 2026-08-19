package uz.tune.mentourBiz.rest.payload.studentRes.res.bookmark;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.BookmarkedQuestion;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResExerciseQuestion;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class ResTeacherBookmark {
    private UUID bookmarkUuid;
    private String comment;
    private Instant createdAt;
//    private String unitTitle;
//    private String taskTitle;
    private ResExerciseQuestion question;

    public ResTeacherBookmark(BookmarkedQuestion bookmark) {
        this.bookmarkUuid = bookmark.getUuid();
        this.comment = bookmark.getComment();
        this.createdAt = bookmark.getCreatedAt();
        if (bookmark.getQuestion() != null) {
            this.question = new ResExerciseQuestion(bookmark.getQuestion());
//            if (bookmark.getQuestion().getExerciseTask() != null) {
//                this.taskTitle = bookmark.getQuestion().getExerciseTask().getTitle();
//                if (bookmark.getQuestion().getExerciseTask().getUnit() != null) {
//                    this.unitTitle = bookmark.getQuestion().getExerciseTask().getUnit().getTitle();
//                }
//            }
        }
    }
}