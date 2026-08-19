package uz.tune.mentourBiz.rest.payload.res.user;


import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.enums.Gender;
import uz.tune.mentourBiz.rest.enums.UserStatus;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;
import uz.tune.mentourBiz.rest.payload.res.school.ResSchoolInfo;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class ResStudentList {
    private UUID studentUuid;
    private UUID userUuid;
    private UserStatus userStatus;
    private ResStudentOne studentOne;
    private String fullName;
    private String username;
    private String phoneNumber;
    private String address;
    private Gender gender;
    private String email;
    private LocalDate dateOfBirth;
    private ResAttachment attachment;
    private ResSchoolInfo respSchoolInfo;
    private boolean hasParent;
    private long parentCount;
    private boolean isAtRisk;
    private Integer overallScore;
    private Integer overallAttendancePercentage;
    private Instant registrationDate;
    private Instant updatedAt;

    public ResStudentList(Student student) {
        this.studentUuid = student.getUuid();
        this.studentOne = new ResStudentOne(student);
        this.userStatus = student.getUser().getStatus();
        if(CoreUtils.isPresent(student.getUser().getAttachment())) {
            this.attachment = new ResAttachment(student.getUser().getAttachment());
        }
        this.userUuid = student.getUser().getUuid();
        this.fullName = student.getUser().getFirstName() + " " + student.getUser().getLastName();
        this.username = student.getUser().getUsername();
        this.phoneNumber = student.getUser().getPhoneNumber();
        this.address = student.getUser().getAddress();
        this.gender = student.getUser().getGender();
        this.email = student.getUser().getEmail();
        this.dateOfBirth = student.getUser().getDateOfBirth();
        this.respSchoolInfo = new ResSchoolInfo(student.getSchool());
        this.hasParent = !student.getParentContacts().isEmpty();
        this.parentCount = student.getParentCount();
        this.isAtRisk = student.getIsAtRisk();
        this.registrationDate = student.getCreatedAt();
        this.updatedAt = student.getUpdatedAt();
    }
}