package uz.tune.mentourBiz.rest.payload.req.student;


import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.Gender;
import uz.tune.mentourBiz.rest.enums.UserStatus;

import java.time.LocalDate;

@Getter
@Setter
public class ReqStudentsUpdate {
    private String firstName;
    private String lastName;
    private UserStatus status;
    private Long currentBalance;
    private String username;
    private String phoneNumber;
    private String address;
    private String password;
    private Gender gender;
    private String email;
    private LocalDate dateOfBirth;
}
