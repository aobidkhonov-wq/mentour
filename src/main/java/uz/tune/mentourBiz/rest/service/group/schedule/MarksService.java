package uz.tune.mentourBiz.rest.service.group.schedule;

import uz.tune.mentourBiz.rest.payload.studentRes.res.lesson.ResMarksDashboard;

import java.util.UUID;

public interface MarksService {
    ResMarksDashboard getMarksForGroup(UUID courseUuid, Integer year, Integer month);
}