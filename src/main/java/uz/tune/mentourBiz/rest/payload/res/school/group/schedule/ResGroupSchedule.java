package uz.tune.mentourBiz.rest.payload.res.school.group.schedule;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.schedule.GroupSchedule;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class ResGroupSchedule {
    private UUID uuid;
    private UUID groupId;
    private String groupName;
    private UUID unitId;
    private String unitTitle;
    private Instant dueDate;

    public ResGroupSchedule(GroupSchedule schedule) {
        this.uuid = schedule.getUuid();
        this.dueDate = schedule.getDueDate();
        if (schedule.getGroup() != null) {
            this.groupId = schedule.getGroup().getUuid();
            this.groupName = schedule.getGroup().getName();
        }
        if (schedule.getUnit() != null) {
            this.unitId = schedule.getUnit().getUuid();
            this.unitTitle = schedule.getUnit().getTitle();
        }
    }
}