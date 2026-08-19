package uz.tune.mentourBiz.rest.payload.res.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.enums.UserStatus;
import uz.tune.mentourBiz.rest.payload.res.ResParentDetail;
import uz.tune.mentourBiz.rest.payload.res.ResRegion;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;
import uz.tune.mentourBiz.rest.payload.res.school.ResSchoolInfo;
import uz.tune.mentourBiz.rest.payload.res.school.group.ResGroup;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResUserInfo {

    private UUID uuid;
    private Instant updatedAt;
    private UUID studentUuid;
    private String fullName;
    private String username;
    private UserRole role;
    private ResSchoolInfo school;
    private ResGroup groups;
    private List<ResGroup> groupList = new ArrayList<>();
    private UserStatus status;
    private List<String> subscriptions = new ArrayList<>();
    private ResAttachment attachment;
    private long totalStudents;
    private long activeClasses;
    private ResRegion region;
    private Long coins;
    private Integer score;
    private Long balance;
    private String organizationName;
    private boolean isLinkedToOrganization;
    private UUID organizationUuid;
    private Instant registrationDate;
    private boolean isAtRisk;
    private String phoneNumber;
    private String address;

    // Student profile page stats
    private Integer attendancePercentage;
    private Integer averageScorePercentage;
    private Long completedLessons;
    private Long totalLessons;
    private Integer rank;
    private Long totalStudentsInSchool;
    private List<ResParentDetail> parentContacts;


    public ResUserInfo(User user, ResSchoolInfo school, ResGroup groups, School schoolEntity) {
        this.updatedAt = user.getUpdatedAt();
        this.uuid = user.getUuid();
        this.registrationDate = user.getCreatedAt();
        this.fullName = user.getFirstName() + " " + user.getLastName();
        this.role = user.getRole();
        this.username = user.getUsername();
        this.school = school;
        this.status = user.getStatus();
//        this.groups = groups;
        this.phoneNumber = user.getPhoneNumber();
        this.address = user.getAddress();
        this.isLinkedToOrganization = schoolEntity != null && schoolEntity.getOrganization() != null;
        this.organizationUuid =
                schoolEntity != null && schoolEntity.getOrganization() != null
                        ? schoolEntity.getOrganization().getUuid()
                        : null;
        if (user.getRole() == UserRole.SYS_ADMIN) {
            this.subscriptions = List.of("ALL");
        }
        if (schoolEntity != null && schoolEntity.getRegion() != null) {
            this.region = new ResRegion(schoolEntity.getRegion());
        }
        if(CoreUtils.isPresent(user.getAttachment())){
            this.attachment = new ResAttachment(user.getAttachment());
        }
        else if (schoolEntity != null && schoolEntity.getActiveSubscriptions() != null) {
            this.subscriptions = schoolEntity.getActiveSubscriptions().stream()
                    .map(Enum::name)
                    .collect(Collectors.toList());
        }
    }

    public ResUserInfo(User user, ResSchoolInfo schoolInfo) {
        this.updatedAt = user.getUpdatedAt();
        this.uuid = user.getUuid();
        this.registrationDate = user.getCreatedAt();
        this.fullName = user.getFirstName() + " " + user.getLastName();
        this.username = user.getUsername();
        this.role = user.getRole();
        this.status = user.getStatus();
        this.phoneNumber = user.getPhoneNumber();
        this.address = user.getAddress();
        this.school = schoolInfo;
        this.isLinkedToOrganization = schoolInfo != null && schoolInfo.isLinkedToOrganization();
        if(CoreUtils.isPresent(user.getAttachment())){
            this.attachment = new ResAttachment(user.getAttachment());
        }
    }

    public ResUserInfo(User user, ResSchoolInfo schoolInfo, ResGroup groupInfo) {
        this.uuid = user.getUuid();
        this.registrationDate = user.getCreatedAt();
        this.fullName = user.getFirstName() + " " + user.getLastName();
        this.username = user.getUsername();
        this.role = user.getRole();
        this.status = user.getStatus();
        this.phoneNumber = user.getPhoneNumber();
        this.address = user.getAddress();
        this.school = schoolInfo;
//        this.groups = groupInfo;
        this.isLinkedToOrganization = schoolInfo != null && schoolInfo.isLinkedToOrganization();
        if(CoreUtils.isPresent(user.getAttachment())){
            this.attachment = new ResAttachment(user.getAttachment());
        }
    }

    public ResUserInfo(User user, ResSchoolInfo school, ResGroup groups, School schoolEntity, Student s, Teacher teacher, long students, long classes) {
        this.uuid = user.getUuid();
        this.updatedAt = user.getUpdatedAt();
        this.registrationDate = user.getCreatedAt();
        if(s != null) {
            this.studentUuid = s.getUuid();
            this.coins = s.getCoins();
            this.score = s.getScore();
            this.balance = s.getCurrentBalance();
        }
        this.phoneNumber = user.getPhoneNumber();
        this.address = user.getAddress();
        this.fullName = user.getFirstName() + " " + user.getLastName();
        this.role = user.getRole();
        this.username = user.getUsername();
        this.school = school;
        this.status = user.getStatus();
//        this.groups = groups;
        this.isLinkedToOrganization = schoolEntity != null && schoolEntity.getOrganization() != null;
        if (user.getRole() == UserRole.SYS_ADMIN) {
            this.subscriptions = List.of("ALL");
        }
        else if (schoolEntity != null && schoolEntity.getActiveSubscriptions() != null) {
            this.subscriptions = schoolEntity.getActiveSubscriptions().stream()
                    .map(Enum::name)
                    .collect(Collectors.toList());
        }
        if(user.getRole().equals(UserRole.TEACHER)) {
            if(teacher.getUser().getAttachment() != null) {
                this.attachment = new ResAttachment(teacher.getUser().getAttachment());
            }
            this.totalStudents = students;
            this.activeClasses = classes;
        }
        if(CoreUtils.isPresent(user.getAttachment())){
            this.attachment = new ResAttachment(user.getAttachment());
        }
    }
}