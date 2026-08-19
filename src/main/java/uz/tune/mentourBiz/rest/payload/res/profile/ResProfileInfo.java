package uz.tune.mentourBiz.rest.payload.res.profile;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.Lang;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.enums.UserStatus;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ResProfileInfo {
    private UUID profileId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String username;
    private Lang language;
    private UserRole role;
    private ResAttachment attachment;
    private UserStatus status;
    private Long coins;
    private Integer score;
    private Long balance;


    public ResProfileInfo(User user) {
        this.profileId = user.getUuid();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.fullName = user.getFirstName() + " " + user.getLastName();
        this.username = user.getUsername();
        this.language = user.getLang();
        this.role = user.getRole();
        this.status = user.getStatus();

        if(CoreUtils.isPresent(user.getAttachment())){
            this.attachment = new ResAttachment(user.getAttachment());
        }
        else{
            attachment = new ResAttachment();
        }
    }

    public ResProfileInfo(User user, Student student) {
        this(user);
        if (student != null) {
            this.coins = student.getCoins();
            this.score = student.getScore();
            this.balance = student.getCurrentBalance();
        }
    }
}