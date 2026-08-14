package uz.tune.mentourBiz.rest.repository.crm;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.tune.mentourBiz.rest.domain.crm.CrmDemoLesson;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CrmDemoLessonRepository extends JpaRepository<CrmDemoLesson, Long> {
    List<CrmDemoLesson> findAllByLessonId(UUID lessonId);

    List<CrmDemoLesson> findAllByLessonIdIn(Collection<UUID> lessonIds);
}
