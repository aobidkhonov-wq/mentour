package uz.tune.mentourBiz.rest.service.lessons;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uz.tune.mentourBiz.rest.enums.LessonStatus;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResUpcomingLesson;

import java.util.List;
import java.util.UUID;

public interface DashboardService {
    Page<ResUpcomingLesson> getUpcomingLessons(Pageable pageable, List<LessonStatus> status, UUID schoolId);
}