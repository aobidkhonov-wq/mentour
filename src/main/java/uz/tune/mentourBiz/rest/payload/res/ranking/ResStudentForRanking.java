package uz.tune.mentourBiz.rest.payload.res.ranking;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;

import java.util.UUID;

@Getter
@Setter
public class ResStudentForRanking {
    private String id;
    private UUID userId;
    private String fullName;
    private Long coinBalance;
    private ResAttachment profilePic;


    public ResStudentForRanking(Student student) {
        this.id = student.getUuid().toString();
        this.userId = student.getUser().getUuid();
        this.fullName = student.getUser().getFirstName() + " " + student.getUser().getLastName();
        this.coinBalance = student.getLifeTimeCoinBalance();
        if (student.getUser().getAttachment() != null) {
            this.profilePic = new ResAttachment(student.getUser().getAttachment());
        } else {
            this.profilePic = null;
        }
    }
}
