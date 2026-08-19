package uz.tune.mentourBiz.rest.repository.school;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.schoolManagement.libriary.LibraryItem;
import uz.tune.mentourBiz.rest.enums.LibraryItemType;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LibraryItemRepository extends BaseRepository<LibraryItem> {

    @Query("SELECT li FROM LibraryItem li " +
            "LEFT JOIN li.school s " +
            "WHERE li.parent IS NULL " +
            "AND (:type IS NULL OR li.type = :type) " +
            "AND (:levelId IS NULL OR li.level.uuid = :levelId) " + // Fix here
            "AND (" +
            "  li.isGlobal = true " +
            "  OR s.uuid IN :schoolUuids " +
            "  OR (li.organization.uuid = :orgUuid AND li.school IS NULL)" +
            ") " +
            "AND (CAST(:title AS string) IS NULL OR LOWER(li.title) LIKE LOWER(CONCAT('%', CAST(:title AS string), '%')))")
    Page<LibraryItem> findLibraryAggregated(
            @Param("type") LibraryItemType type,
            @Param("levelId") UUID levelId, // Now correctly handles NULL as its own category
            @Param("schoolUuids") Collection<UUID> schoolUuids,
            @Param("orgUuid") UUID orgUuid,
            @Param("title") String title,
            Pageable pageable);

    Optional<LibraryItem> findByUuid(UUID uuid);

}