package uz.tune.mentourBiz.rest.payload.res.school.group;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;

import java.util.UUID;

@Getter
@Setter
public class ResLevelInfoForGroup {
    private String level;
    private UUID uuid;
    private Long subjectId;
    private String subjectName;

    public ResLevelInfoForGroup(Group group) {
        this.level = group.getLevel().getName();
        this.uuid = group.getLevel().getUuid();
        this.subjectId = group.getLevel().getSubject() != null
                ? group.getLevel().getSubject().getId()
                : null;
        this.subjectName=group.getLevel().getSubject() != null
                ? group.getLevel().getSubject().getName()
                : null;
    }
}
