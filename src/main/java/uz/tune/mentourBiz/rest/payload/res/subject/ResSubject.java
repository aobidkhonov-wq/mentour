package uz.tune.mentourBiz.rest.payload.res.subject;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Subject;

@Getter
@Setter
public class ResSubject {
    private Long id;
    private String name;
    private Boolean isGlobal;

    public ResSubject(Subject subject) {
        this.id = subject.getId();
        this.name = subject.getName();
        this.isGlobal = subject.getIsGlobal();
    }
}
