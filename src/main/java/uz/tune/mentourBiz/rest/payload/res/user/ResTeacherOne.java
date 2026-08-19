package uz.tune.mentourBiz.rest.payload.res.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;
import uz.tune.mentourBiz.rest.payload.res.school.ResSchoolInfo;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ResTeacherOne {
    private UUID uuid;
    private String username;
    private String fullName;
    private UserRole role = UserRole.TEACHER;
    private ResSchoolInfo school;
    private ResAttachment attachment;
    private Integer totalStudents;
    private Integer activeClasses;

    // Original constructor
    public ResTeacherOne(Teacher teacher) {
        this.uuid = teacher.getUser().getUuid();
        this.username = teacher.getUser().getUsername();
        this.fullName = teacher.getUser().getFirstName() + " " + teacher.getUser().getLastName();
        if (teacher.getSchool() != null) {
            this.school = new ResSchoolInfo(teacher.getSchool());
        }
    }

    // New UI constructor
    public ResTeacherOne(Teacher teacher, long totalStudents, long activeClasses) {
        this(teacher);
        this.totalStudents = (int) totalStudents;
        this.activeClasses = (int) activeClasses;
        if (teacher.getUser().getAttachment() != null) {
            this.attachment = new ResAttachment(teacher.getUser().getAttachment());
        }
    }
}