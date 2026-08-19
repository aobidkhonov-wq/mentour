package uz.tune.mentourBiz.rest.repository.group;

import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.BillingPlan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillingPlanRepository extends BaseRepository<BillingPlan> {

    Optional<BillingPlan> findByUuid(UUID uuid);

    List<BillingPlan> findAllByIsActiveTrue();

    // School-scoped lookups: a plan is only visible/editable within its own school.
    List<BillingPlan> findAllBySchool_Uuid(UUID schoolUuid);

    List<BillingPlan> findAllBySchool_UuidAndIsActiveTrue(UUID schoolUuid);

    Optional<BillingPlan> findByUuidAndSchool_Uuid(UUID uuid, UUID schoolUuid);
}
