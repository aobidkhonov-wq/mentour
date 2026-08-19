package uz.tune.mentourBiz.rest.repository.school;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.enums.SchoolStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolRepo extends BaseRepository<School> {
    Page<School> findAllByStatus(SchoolStatus status, Pageable pageable);

    Page<School> findAllByUuidAndStatus(UUID uuid, SchoolStatus status, Pageable pageable);
    List<School> findAllByUuidAndStatus(UUID uuid, SchoolStatus status);

    List<School> findAllByUuidIn(List<UUID> uuid);
    Optional<School> findByUuid(UUID schoolId);
    List<School> findAllByStatus(SchoolStatus status);

    @Query("SELECT s.uuid FROM School s WHERE s.status = 'ACTIVE'")
    List<UUID> findAllActiveSchoolUuids();
    List<School> findAllByStatusAndUpdatedAtBefore(SchoolStatus status, Instant timestamp);

//
@Query("""
    SELECT s FROM School s
    WHERE (:status IS NULL OR s.status = :status)
    AND (CAST(:schoolName AS string) IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:schoolName AS string), '%')))
    AND (COALESCE(:uuids, NULL) IS NULL OR s.uuid IN :uuids)
""")
Page<School> findWithFilters(
        @Param("status") SchoolStatus status,
        @Param("schoolName") String schoolName,
        @Param("uuids") Collection<UUID> uuids, // Changed from List to Collection
        Pageable pageable
);

    @Query("""
    SELECT s FROM School s
    WHERE s.status = 'ACTIVE'
    AND NOT EXISTS (
        SELECT li FROM LibraryItem li 
        WHERE li.school = s
    )
""")
    List<School> findSchoolsWithEmptyLibrary();

    @Query("""
    SELECT s, 
           (SELECT COUNT(p) FROM PaymentPackage p WHERE p.school = s),
           (SELECT COUNT(li) FROM LibraryItem li WHERE li.school = s),
           (SELECT COUNT(si) FROM ShopItem si WHERE si.school = s)
    FROM School s 
    WHERE s.status = 'ACTIVE'
""")
    List<Object[]> findAllSchoolReadinessStats();

}