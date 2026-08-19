package uz.tune.mentourBiz.rest.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.PaymentPackage;
import uz.tune.mentourBiz.rest.enums.SchoolStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentPackageRepo extends BaseRepository<PaymentPackage> {

    Optional<PaymentPackage> findByUuid(UUID uuid);
    List<PaymentPackage> findAllByPaymentDueDateAndSchoolStatus(Integer day, SchoolStatus status);

    @Query("SELECT p FROM PaymentPackage p WHERE p.school.uuid IN :schoolUuids AND (:name IS NULL OR p.name = :name)")
    List<PaymentPackage> findAllBySchool_UuidInAndName(@Param("schoolUuids") Collection<UUID> schoolUuids, @Param("name") String name);
}