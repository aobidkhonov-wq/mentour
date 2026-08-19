package uz.tune.mentourBiz.rest.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.Organization;
import uz.tune.mentourBiz.rest.enums.SchoolStatus;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends BaseRepository<Organization> {
    Optional<Organization> findByUuid(UUID uuid);
    Page<Organization> findAllByStatusNot(SchoolStatus status, Pageable pageable);
}