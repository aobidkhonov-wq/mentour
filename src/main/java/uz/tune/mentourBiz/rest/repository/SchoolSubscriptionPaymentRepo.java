package uz.tune.mentourBiz.rest.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.external.octo.SchoolSubscriptionPayment;
import uz.tune.mentourBiz.rest.enums.TransactionStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolSubscriptionPaymentRepo extends BaseRepository<SchoolSubscriptionPayment> {
    Optional<SchoolSubscriptionPayment> findByShopTransactionId(String shopTransactionId);
    List<SchoolSubscriptionPayment> findAllByStatusAndUzumTransactionIdNotNullAndCreatedAtAfter(TransactionStatus status, Instant createdAt);
    Page<SchoolSubscriptionPayment> findAllBySchool_Uuid(UUID schoolUuid, Pageable pageable);
    Page<SchoolSubscriptionPayment> findAllByStatus(TransactionStatus status, Pageable pageable);
    Page<SchoolSubscriptionPayment> findAllBySchool_UuidIn(Collection<UUID> schoolUuids, Pageable pageable);
}