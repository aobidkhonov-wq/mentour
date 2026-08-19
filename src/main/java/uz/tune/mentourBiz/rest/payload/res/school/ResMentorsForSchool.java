package uz.tune.mentourBiz.rest.payload.res.school;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.userManagement.user.SchoolMentor;

import java.util.UUID;

@Getter
@Setter
public class ResMentorsForSchool {
    private UUID schoolMentorId;
    private UUID mentorId;
    private String mentorFullName;
    private Integer contractHours;

    public ResMentorsForSchool(SchoolMentor schoolMentor) {
        this.schoolMentorId = schoolMentor.getUuid();
        this.mentorId = schoolMentor.getMentor().getUser().getUuid();
        this.mentorFullName = schoolMentor.getMentor().getUser().getFirstName() + " " + schoolMentor.getMentor().getUser().getLastName();
        this.contractHours = schoolMentor.getContractHours();
    }
}