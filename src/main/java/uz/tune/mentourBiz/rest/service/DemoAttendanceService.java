package uz.tune.mentourBiz.rest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.rest.domain.crm.CrmDemoLesson;
import uz.tune.mentourBiz.rest.domain.crm.CrmLead;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.CourseLesson;
import uz.tune.mentourBiz.rest.enums.DemoAttendanceStatus;
import uz.tune.mentourBiz.rest.payload.req.schedule.ReqMarkDemoAttendance;
import uz.tune.mentourBiz.rest.payload.res.demo.ResDemoAttendanceRecord;
import uz.tune.mentourBiz.rest.repository.course.CourseLessonRepo;
import uz.tune.mentourBiz.rest.repository.crm.CrmDemoLessonRepository;
import uz.tune.mentourBiz.rest.repository.crm.CrmLeadRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DemoAttendanceService {

    private final CrmDemoLessonRepository crmDemoLessonRepo;
    private final CrmLeadRepository crmLeadRepo;
    private final CourseLessonRepo courseLessonRepo;

    public List<ResDemoAttendanceRecord> getByLesson(UUID lessonUuid) {
        List<CrmDemoLesson> entries = crmDemoLessonRepo.findAllByLessonId(lessonUuid);
        if (entries.isEmpty()) return List.of();

        boolean lessonEnded = isLessonEnded(lessonUuid);
        List<ResDemoAttendanceRecord> result = new ArrayList<>();
        for (CrmDemoLesson entry : entries) {
            CrmLead lead = entry.getLead();
            if (lead == null || lead.getUuid() == null) continue;
            result.add(new ResDemoAttendanceRecord(lead, lessonEnded));
        }
        return result;
    }

    public Map<String, List<ResDemoAttendanceRecord>> getByLessons(List<UUID> lessonUuids) {
        if (lessonUuids == null || lessonUuids.isEmpty()) return Map.of();

        List<CrmDemoLesson> entries = crmDemoLessonRepo.findAllByLessonIdIn((Collection<UUID>) lessonUuids);
        Map<String, List<ResDemoAttendanceRecord>> result = new HashMap<>();
        for (UUID lu : lessonUuids) result.put(lu.toString(), new ArrayList<>());

        // cache lesson end-time lookups
        Map<UUID, Boolean> endedCache = new HashMap<>();

        for (CrmDemoLesson entry : entries) {
            CrmLead lead = entry.getLead();
            if (lead == null || lead.getUuid() == null) continue;

            UUID lessonUuid = entry.getLessonId();
            boolean lessonEnded = endedCache.computeIfAbsent(lessonUuid, this::isLessonEnded);

            result.computeIfAbsent(lessonUuid.toString(), k -> new ArrayList<>())
                    .add(new ResDemoAttendanceRecord(lead, lessonEnded));
        }
        return result;
    }

    @Transactional
    public ResDemoAttendanceRecord mark(UUID crmLeadUuid, ReqMarkDemoAttendance req) {
        CrmLead lead = crmLeadRepo.findByUuid(crmLeadUuid)
                .orElseThrow(() -> new EntityNotFoundException("CRM lead not found: " + crmLeadUuid));

        String demoStatus = req.getStatus() == DemoAttendanceStatus.PRESENT ? "ATTENDED" : "NO_SHOW";
        lead.setDemoStatus(demoStatus);
        crmLeadRepo.save(lead);

        return new ResDemoAttendanceRecord(lead, true);
    }

    private boolean isLessonEnded(UUID lessonUuid) {
        return courseLessonRepo.findByUuid(lessonUuid)
                .map(CourseLesson::getEndTime)
                .map(end -> end.isBefore(Instant.now()))
                .orElse(false);
    }
}
