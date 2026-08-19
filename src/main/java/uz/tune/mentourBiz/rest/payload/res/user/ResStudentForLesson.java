package uz.tune.mentourBiz.rest.payload.res.user;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;

import java.util.UUID;

@Getter
@Setter
public class ResStudentForLesson {
    private UUID studentId;
    private String fullName;
    private UUID userUuid;

    public ResStudentForLesson(Student student) {
        this.studentId = student.getUuid();
        this.userUuid = student.getUser().getUuid();
        this.fullName = student.getUser().getFirstName() + " " + student.getUser().getLastName();
    }
}