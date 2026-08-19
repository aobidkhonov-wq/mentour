package uz.tune.mentourBiz.rest.payload.res.level;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Level;

import java.util.UUID;

@Getter
@Setter
public class ResLevel {
    private UUID uuid;
    private String name;
    private Long subjectId;
    private String subjectName;

    public ResLevel(Level level) {
        this.uuid = level.getUuid();
        this.name = level.getName();
        this.subjectId = level.getSubject() != null ? level.getSubject().getId() : null;
        this.subjectName = level.getSubject() != null ? level.getSubject().getName() : null;
    }
}
