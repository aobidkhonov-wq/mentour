package uz.tune.mentourBiz.rest.payload.res.user;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.enums.EnrollmentStatus;
import uz.tune.mentourBiz.rest.enums.Gender;
import uz.tune.mentourBiz.rest.payload.res.school.ResSchoolInfo;
import uz.tune.mentourBiz.rest.payload.res.school.group.ResGroup;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
public class ResStudentOne {
    private String id;
    private UUID userUuid;
    private String fullName;
    private String telegram;
    private Long coins;
    private ResSchoolInfo studentSchool;
    private List<ResGroup> groups;
    private Long parentCount;
    private Boolean atRisk;
    private Gender gender;
    private String email;
    private LocalDate dateOfBirth;
    private Instant createdAt;
    private Instant updatedAt;

    public ResStudentOne(Student student) {
        this.id = student.getUuid().toString();
        this.userUuid = student.getUser().getUuid();
        this.coins = student.getCoins();
        this.fullName = student.getUser().getFirstName() + " " + student.getUser().getLastName();
        this.studentSchool = new ResSchoolInfo(student.getSchool());
        this.createdAt = student.getCreatedAt();
        this.createdAt = student.getUpdatedAt();
        this.parentCount = student.getParentCount();
        this.atRisk = student.getIsAtRisk();
        this.gender = student.getUser().getGender();
        this.email = student.getUser().getEmail();
        this.dateOfBirth = student.getUser().getDateOfBirth();
        if (student.getEnrollments() != null && student.getEnrollments().stream().anyMatch(e -> e.getStatus().equals(EnrollmentStatus.ONGOING))) {
            this.groups = student.getEnrollments().stream()
                    .filter(e -> e.getStatus().equals(EnrollmentStatus.ONGOING))
                    .map(Enrollment::getGroup)
                    .map(ResGroup::new)
                    .collect(Collectors.toList());
        }
    }
}