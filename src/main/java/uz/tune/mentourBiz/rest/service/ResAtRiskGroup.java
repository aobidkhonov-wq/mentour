package uz.tune.mentourBiz.rest.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ResAtRiskGroup {
    private UUID uuid;
    private String name;
    private List<ResAtRiskStudent> students = new ArrayList<>();

    public ResAtRiskGroup(Group group) {
        this.uuid = group.getUuid();
        this.name = group.getName();
        this.students = new ArrayList<>();
    }

    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResAtRiskStudent {
        private UUID uuid;
        private String name;
    }
}
