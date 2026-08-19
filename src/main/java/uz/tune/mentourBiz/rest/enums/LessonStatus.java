package uz.tune.mentourBiz.rest.enums;

import java.time.Instant;

public enum LessonStatus {
    NEW,
    STARTED,
    FINISHED,
    DELETED,

    //

    STUDENT_APP,
    ;

    /**
     * Nothing flips a lesson to FINISHED in the database (only finishing the whole course does), so
     * a lesson whose end time has passed is reported as FINISHED to clients. Responses carry times
     * shifted into the school's local wall clock, so clients cannot derive this themselves.
     */
    public static LessonStatus effective(LessonStatus status, Instant endTime) {
        if (status == null || endTime == null || status == DELETED || status == FINISHED) {
            return status;
        }
        return endTime.isBefore(Instant.now()) ? FINISHED : status;
    }
}
