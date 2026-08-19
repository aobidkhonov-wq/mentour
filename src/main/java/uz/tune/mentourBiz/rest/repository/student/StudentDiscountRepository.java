package uz.tune.mentourBiz.rest.repository.student;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.userManagement.user.StudentDiscount;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentDiscountRepository extends BaseRepository<StudentDiscount> {

    Optional<StudentDiscount> findByUuid(UUID uuid);

    /** Everything ever granted to the student, newest first — the history screen. */
    List<StudentDiscount> findAllByStudent_UuidOrderByCreatedAtDesc(UUID studentUuid);

    /**
     * The discounts in force for the student on {@code onDate}. Only one is expected (creating a second
     * overlapping one is refused), but the query returns a list so an older data set with overlaps still
     * resolves deterministically — newest first, and the caller takes the first.
     */
    @Query("""
        SELECT d FROM StudentDiscount d
        WHERE d.student.uuid = :studentUuid
          AND d.isActive = true
          AND d.startDate <= :onDate
          AND (d.endDate IS NULL OR d.endDate > :onDate)
        ORDER BY d.createdAt DESC, d.id DESC
    """)
    List<StudentDiscount> findActiveForStudent(
            @Param("studentUuid") UUID studentUuid,
            @Param("onDate") LocalDate onDate);

    /** Same as {@link #findActiveForStudent} for a whole page of students, in one query. */
    @Query("""
        SELECT d FROM StudentDiscount d
        WHERE d.student.uuid IN :studentUuids
          AND d.isActive = true
          AND d.startDate <= :onDate
          AND (d.endDate IS NULL OR d.endDate > :onDate)
        ORDER BY d.createdAt DESC, d.id DESC
    """)
    List<StudentDiscount> findActiveForStudents(
            @Param("studentUuids") Collection<UUID> studentUuids,
            @Param("onDate") LocalDate onDate);
}
