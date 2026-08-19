package uz.tune.mentourBiz.rest.payload.req.student;


import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.Gender;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ReqStudentCreate {
    private UUID schoolUuid;
    private String firstName;
    private String lastName;
    private String username;
    private String password;
    private List<UUID> groupUuids;
    private Long currentBalance;
    private String phoneNumber;
    private Gender gender;
    private String email;
    private LocalDate dateOfBirth;
}
