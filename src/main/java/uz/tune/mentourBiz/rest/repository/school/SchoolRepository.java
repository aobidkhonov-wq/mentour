package uz.tune.mentourBiz.rest.repository.school;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.enums.SchoolStatus;

import java.util.Optional;
import java.util.UUID;

public interface SchoolRepository extends BaseRepository<School> {
    Optional<School> findByUuid(UUID uuid);

    Page<School> findAllByStatus(SchoolStatus status, Pageable pageable);
}