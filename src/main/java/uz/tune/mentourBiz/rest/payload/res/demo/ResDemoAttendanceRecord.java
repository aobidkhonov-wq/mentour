package uz.tune.mentourBiz.rest.payload.res.demo;

import lombok.Getter;
import uz.tune.mentourBiz.rest.domain.crm.CrmLead;
import uz.tune.mentourBiz.rest.enums.DemoAttendanceStatus;

import java.util.UUID;

@Getter
public class ResDemoAttendanceRecord {

    private final UUID uuid;
    private final String contactName;
    private final String contactPhone;
    private final UUID crmLeadUuid;
    private final DemoAttendanceStatus status;
    private final String note;

    public ResDemoAttendanceRecord(CrmLead lead, boolean lessonEnded) {
        this.uuid = lead.getUuid();
        this.crmLeadUuid = lead.getUuid();
        this.contactName = lead.resolvedName();
        this.contactPhone = lead.resolvedPhone();
        this.status = mapStatus(lead.getDemoStatus(), lessonEnded);
        this.note = null;
    }

    private static DemoAttendanceStatus mapStatus(String demoStatus, boolean lessonEnded) {
        if ("ATTENDED".equals(demoStatus)) return DemoAttendanceStatus.PRESENT;
        if ("NO_SHOW".equals(demoStatus)) return DemoAttendanceStatus.ABSENT;
        // SCHEDULED or null — use lesson end time to decide
        return lessonEnded ? DemoAttendanceStatus.ABSENT : DemoAttendanceStatus.NOT_MARKED;
    }
}
