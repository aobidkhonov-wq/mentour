package uz.tune.mentourBiz.rest.repository.shop;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.shopManagement.coins.ItemRedemption;
import uz.tune.mentourBiz.rest.enums.ItemRedemptionStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemRedemptionRepository extends BaseRepository<ItemRedemption> {

    Page<ItemRedemption> findAllByShopItem_School_UuidIn(Collection<UUID> schoolUuid, Pageable pageable);
    Optional<ItemRedemption> findByUuid(UUID uuid);
    Page<ItemRedemption> findAllByStudent_Uuid(UUID studentUuid, Pageable pageable);
    @Query("""
    SELECT COUNT(ir)
    FROM ItemRedemption ir
    WHERE ir.status = 'PENDING'
      AND ir.shopItem.school.uuid IN :schoolUuids
""")
    long countPendingOrdersIn(@Param("schoolUuids") List<UUID> schoolUuids);

    @Query("""
    SELECT ir FROM ItemRedemption ir 
    JOIN ir.student s 
    JOIN s.user u 
    JOIN ir.shopItem item 
    WHERE (:status IS NULL OR ir.status = :status) 
    AND (COALESCE(:schoolUuids, NULL) IS NULL OR item.school.uuid IN :schoolUuids) 
    AND (:classUuid IS NULL OR EXISTS (
        SELECT 1 FROM Enrollment e 
        WHERE e.student = s AND e.group.uuid = :classUuid 
        AND e.status = 'ONGOING'
    )) 
    AND (:teacherUuid IS NULL OR EXISTS (
        SELECT 1 FROM Enrollment e2 
        WHERE e2.student = s AND e2.group.teacher.user.uuid = :teacherUuid 
        AND e2.status = 'ONGOING'
    )) 
    AND (:productName IS NULL OR LOWER(CAST(item.name AS string)) LIKE :productName) 
    AND (:clientName IS NULL OR LOWER(CAST(CONCAT(u.firstName, ' ', u.lastName) AS string)) LIKE :clientName)
""")
    Page<ItemRedemption> findAllOrdersFilteredMulti(
            @Param("status") ItemRedemptionStatus status,
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("classUuid") UUID classUuid,
            @Param("teacherUuid") UUID teacherUuid,
            @Param("productName") String productName,
            @Param("clientName") String clientName,
            Pageable pageable);
}
