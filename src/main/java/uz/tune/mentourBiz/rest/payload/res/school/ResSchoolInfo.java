package uz.tune.mentourBiz.rest.payload.res.school;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.enums.SchoolStatus;
import uz.tune.mentourBiz.rest.payload.res.ResBooks;
import uz.tune.mentourBiz.rest.payload.res.ResRegion;
import uz.tune.mentourBiz.rest.payload.res.ResSchoolSubscription;
import uz.tune.mentourBiz.rest.payload.res.ResSubscriptionPlan;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResSchoolInfo {
    private UUID uuid;
    private ResAttachment logo;
    private String name;
    private String address;
    private Integer studentCount;
    private Integer classCount;
    private String phoneNumber;
    private Integer teacherCount;
    private String telegramLink;
    private SchoolStatus status;
    private Integer maxStudents;
    private List<ResBooks> schoolBookList;
    private ResRegion regionDetails;
    private Boolean isPaymentActive;
    private ResSubscriptionPlan resSubscriptionPlan;
    private ResSchoolSubscription subscription;
    private boolean isLinkedToOrganization;
    private java.time.Instant lastLessonCreatedAt;
    private java.time.Instant latestDueDate;
    // Update the constructor to map the missing fields
    public ResSchoolInfo(School school) {
        if (school != null) {
            this.uuid = school.getUuid();
            this.name = school.getName();
            this.status = school.getStatus();
            this.address = school.getAddress();
            this.isPaymentActive = school.isPaymentActive();
            this.isLinkedToOrganization = school.getOrganization() != null;
            this.maxStudents = school.getStudentLimit();
            if (school.getLogo() != null) {
                this.logo = new ResAttachment(school.getLogo());
            }
            if (school.getRegion() != null) {
                this.regionDetails = new ResRegion(school.getRegion());
            }
            if(school.getSubscriptionPlan() != null){
                this.resSubscriptionPlan = new ResSubscriptionPlan(school.getSubscriptionPlan());
            }
            this.studentCount = school.getStudentCount();
            this.classCount = school.getClassCount();
            this.phoneNumber = school.getContactInfo();
            this.teacherCount = school.getTeacherCount();
            this.telegramLink = school.getTelegramLink();
            if (school.getAllowedBooks() != null) {
                this.schoolBookList = school.getAllowedBooks().stream()
                        .map(ResBooks::new)
                        .collect(Collectors.toList());
            }
        }
    }
    public ResSchoolInfo(School school, ResSchoolSubscription subscription) {
        this(school);
        this.subscription = subscription;
    }
}