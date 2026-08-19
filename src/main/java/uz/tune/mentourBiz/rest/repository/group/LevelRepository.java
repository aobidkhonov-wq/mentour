package uz.tune.mentourBiz.rest.repository.group;

import org.springframework.data.domain.Sort;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Level;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LevelRepository extends BaseRepository<Level> {
    Optional<Level> findByUuid(UUID uuid);
    List<Level> findAll();
    List<Level> findAllBySubject_Id(Long subjectId, Sort sort);
    boolean existsBySubject_Id(Long subjectId);
}
