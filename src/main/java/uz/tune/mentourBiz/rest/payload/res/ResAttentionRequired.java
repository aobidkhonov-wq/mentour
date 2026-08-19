package uz.tune.mentourBiz.rest.payload.res;

import lombok.Builder;
import lombok.Data;
import uz.tune.mentourBiz.rest.payload.res.school.group.ResGroup;
import uz.tune.mentourBiz.rest.payload.res.user.ResStudentList;
import uz.tune.mentourBiz.rest.payload.res.course.ResLessonDetail;

import java.util.List;

@Data
@Builder
public class ResAttentionRequired {
    // debt students balance < 0
    private long overdueCount;
    private List<ResStudentList> overdueStudents;

    // gr without hw
    private long groupsNoHomeworkCount;
    private List<ResGroup> groupsNoHomework;

    //  miss attendanc lesson passed but not marked
    private long missingAttendanceCount;
    private List<ResLessonDetail> missingAttendanceLessons;

    // active status but no ongoing enrollment
    private long unassignedStudentsCount;
    private List<ResStudentList> unassignedStudents;
}