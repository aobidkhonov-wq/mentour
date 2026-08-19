package uz.tune.mentourBiz.rest.payload.req.school;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ReqAssignMentorToSchool {
    private UUID schoolId;
    private UUID mentorId;
    private Integer contractHours;
}