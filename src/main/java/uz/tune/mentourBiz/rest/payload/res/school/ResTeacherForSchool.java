package uz.tune.mentourBiz.rest.payload.res.school;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;


import java.util.UUID;

@Getter
@Setter
public class ResTeacherForSchool {
    private UUID teacherId;
    private String mentorFullName;

    public ResTeacherForSchool(Teacher schoolMentor) {
        this.teacherId = schoolMentor.getUser().getUuid();
        this.mentorFullName = schoolMentor.getUser().getFirstName() + " " + schoolMentor.getUser().getLastName();
    }
}