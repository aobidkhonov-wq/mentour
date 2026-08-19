package uz.tune.mentourBiz.rest.repository.school;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.Branch;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface BranchRepository extends BaseRepository<Branch> {
    Optional<Branch> findByUuid(UUID uuid);
    Optional<Branch> findBySchool_Uuid(UUID schoolUuid);
    Page<Branch> findAllBySchool_Uuid(UUID schoolUuid, Pageable pageable);
    Page<Branch> findAllBySchool_UuidIn(Collection<UUID> schoolUuids, Pageable pageable);
}
