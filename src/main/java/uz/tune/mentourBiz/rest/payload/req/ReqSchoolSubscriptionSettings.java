package uz.tune.mentourBiz.rest.payload.req;

import lombok.Data;

@Data
public class ReqSchoolSubscriptionSettings {
    private Boolean autoFreezeEnabled;
    private Integer overdueDaysToFreeze;
    private Boolean aiExerciseEnabled;
    private Boolean aiWritingEnabled;
    private Boolean aiSpeakingEnabled;
}