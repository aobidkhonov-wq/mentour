package uz.tune.mentourBiz.rest.payload.res.studentApp;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Level;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.Lang;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.enums.UserStatus;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;
import uz.tune.mentourBiz.rest.payload.res.level.ResLevel;
import uz.tune.mentourBiz.rest.payload.res.school.ResSchoolInfo;

import java.util.UUID;

@Getter
@Setter
public class ResStudentHomeProfile {
    private UUID userId;
    private UUID studentId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String username;
    private Lang language;
    private UserRole role;
    private UserStatus status;
    private String schoolName;
    private Long coins;
    private Integer score;
    private Integer rankingPosition;
    private String rankingLabel;
    private ResLevel level;
    private ResAttachment profilePhoto;
    private ResSchoolInfo schoolInfo;
    private Long balance;

    public ResStudentHomeProfile() {
    }

    public ResStudentHomeProfile(Student student, Integer rankingPosition, String rankingLabel, School school, Level level) {
        User user = student.getUser();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.fullName = user.getFirstName() + " " + user.getLastName();
        this.username = user.getUsername();
        this.language = user.getLang();
        this.role = user.getRole();
        this.status = user.getStatus();
        if (student.getSchool() != null) {
            this.schoolName = student.getSchool().getName();
        }
        this.studentId = student.getUuid();
        this.userId = student.getUser().getUuid();
        this.coins = student.getCoins();
        this.score = student.getScore();
        this.level = new ResLevel(level);
        this.rankingPosition = rankingPosition;
        this.rankingLabel = rankingLabel;
        if (user.getAttachment() != null) {
            this.profilePhoto = new ResAttachment(user.getAttachment());
        }
        this.schoolInfo = new ResSchoolInfo(school);
        this.balance = student.getCurrentBalance();
    }
}