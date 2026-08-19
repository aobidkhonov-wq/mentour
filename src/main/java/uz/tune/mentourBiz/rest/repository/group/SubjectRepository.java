package uz.tune.mentourBiz.rest.repository.group;

import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Subject;

public interface SubjectRepository extends BaseRepository<Subject> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
