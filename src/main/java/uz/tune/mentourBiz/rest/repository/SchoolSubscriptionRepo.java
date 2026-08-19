package uz.tune.mentourBiz.rest.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.SchoolSubscription;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SchoolSubscriptionRepo extends BaseRepository<SchoolSubscription> {
    Optional<SchoolSubscription> findBySchool_Uuid(UUID schoolUuid);

    Page<SchoolSubscription> findAllByExpiresAtBeforeAndExpiresAtAfter(Instant threshold, Instant now, Pageable pageable);
}
