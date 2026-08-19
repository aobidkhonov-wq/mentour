package uz.tune.mentourBiz.rest.repository;

import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.SubscriptionPlan;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionPlanRepo extends BaseRepository<SubscriptionPlan> {
    Optional<SubscriptionPlan> findByUuid(UUID uuid);
    Optional<SubscriptionPlan> findByName(String name);
}