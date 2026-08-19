package uz.tune.mentourBiz.rest.service.writing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uz.tune.mentourBiz.rest.enums.ExerciseSubmissionStatus;
import uz.tune.mentourBiz.rest.payload.req.writing.ReqTeacherFeedBackForWriting;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.exercise.ResWriting;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResWritingList;

import java.util.UUID;

public interface WritingService {
    ResWriting getWritingSubmission(UUID submissionUuid);
    ResponseMessage rejectSubmission(UUID submissionUuid);
    ResWriting getStudentsWriting(UUID studentId, UUID questionUuid);
    ResponseMessage gradeAndGiveFeedback(UUID submissionUUID, ReqTeacherFeedBackForWriting reqTeacherFeedBackForWriting);
    Page<ResWritingList> getAllSubmissions(ExerciseSubmissionStatus status, UUID studentUuid, UUID groupUuid, Pageable pageable);
    ResponseMessage triggerAiReview(UUID submissionUuid);
    void processAiResult(UUID submissionUuid, Integer score, String feedback, Integer grammar, Integer vocab, Integer coherence);
}