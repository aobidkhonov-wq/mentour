package uz.tune.mentourBiz.rest.repository.shop;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.shopManagement.ShopItem;
import uz.tune.mentourBiz.rest.enums.ShopItemType;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ShopItemRepository extends BaseRepository<ShopItem> {
    Optional<ShopItem> findByUuid(UUID uuid);

    @Query("""
    SELECT si FROM ShopItem si
    LEFT JOIN si.organization org
    LEFT JOIN si.school sch
    WHERE si.isActive = true
    AND (
        (org IS NULL AND sch IS NULL)
        OR
        (org.uuid = :orgUuid AND sch IS NULL)
        OR
        (sch.uuid IN :schoolUuids)
    )
    AND (:type = uz.tune.mentourBiz.rest.enums.ShopItemType.ALL OR si.type = :type)
""")
    Page<ShopItem> findAllAggregated(
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("orgUuid") UUID orgUuid,
            @Param("type") ShopItemType type,
            Pageable pageable);

//    @Query("""
//        SELECT si FROM ShopItem si
//        WHERE si.isActive = true
//        AND (
//            si.school.uuid IN :schoolUuids
//            OR (si.organization.uuid = :orgUuid AND si.school IS NULL)
//        )
//        AND (:type = 'ALL' OR si.type = :type)
//    """)
//    Page<ShopItem> findAllAggregated(
//            @Param("schoolUuids") Collection<UUID> schoolUuids,
//            @Param("orgUuid") UUID orgUuid,
//            @Param("type") ShopItemType type,
//            Pageable pageable);

}
