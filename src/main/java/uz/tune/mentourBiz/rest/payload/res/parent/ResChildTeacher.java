package uz.tune.mentourBiz.rest.payload.res.parent;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ResChildTeacher {
    private UUID uuid;
    private String fullName;
    private String phoneNumber;
    private ResAttachment photo;
    private String teacherType;

    public ResChildTeacher(Teacher teacher) {
        User user = teacher.getUser();
        this.uuid = user.getUuid();
        this.fullName = user.getFirstName() + " " + user.getLastName();
        this.phoneNumber = (user.getPhoneNumber() != null) ? user.getPhoneNumber() : user.getUsername();
        this.photo = (user.getAttachment() != null) ? new ResAttachment(user.getAttachment()) : null;
        this.teacherType = (user.getRole() != null) ? user.getRole().name() : null;
    }
}
