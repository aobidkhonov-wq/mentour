package uz.tune.mentourBiz.rest.payload.req.course;


import lombok.Getter;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;

import java.util.List;

@Getter
public class SegmentDescriptor {
    private final Student student;
    private final List<long[]> timeRanges;

    public SegmentDescriptor(Student student, List<long[]> timeRanges) {
        this.student = student;
        this.timeRanges = timeRanges;
    }
}