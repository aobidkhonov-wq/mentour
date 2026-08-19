package uz.tune.mentourBiz.rest.repository.group.enrollment;

import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.CourseLesson;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.EnrollmentLessonConsumption;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnrollmentLessonConsumptionRepository extends BaseRepository<EnrollmentLessonConsumption> {

    boolean existsByEnrollmentAndLesson(Enrollment enrollment, CourseLesson lesson);

    long countByEnrollmentAndExcusedFalse(Enrollment enrollment);

    Optional<EnrollmentLessonConsumption> findByEnrollment_UuidAndLesson_Uuid(UUID enrollmentUuid, UUID lessonUuid);
}
