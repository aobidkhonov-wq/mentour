package uz.tune.mentourBiz.rest.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FailedStudentModel {
    private String firstName;
    private String lastName;
    private String middleName;
    private String groupId;
    private String reason;
}