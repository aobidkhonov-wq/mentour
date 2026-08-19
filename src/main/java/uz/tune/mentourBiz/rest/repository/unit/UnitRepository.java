package uz.tune.mentourBiz.rest.repository.unit;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.enums.UnitStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UnitRepository extends BaseRepository<Unit> {

    Optional<Unit> findByUuid(UUID uuid);

    List<Unit> findAllByUuidIn(Collection<UUID> uuids);
//
    List<Unit> findAllBySchoolBookUuid(UUID bookUuid);
    @Query("""
       SELECT u FROM Unit u
       WHERE u.schoolBook.uuid = :bookUuid
       AND (:status IS NULL OR u.status = :status)
       """)
    List<Unit> findAllBySchoolBookUuidAndStatus(UUID bookUuid, UnitStatus status);
    List<Unit> findAllBySchoolBookUuidOrderBySortOrderAsc(UUID bookUuid);
    boolean existsByTitleAndSchoolBookId(String title, Long schoolBookId);

    @Query("SELECT DISTINCT u.id " +
            "FROM CourseLesson cl " +
            "JOIN cl.units u " +
            "WHERE cl.course.uuid = :courseUuid AND cl.status = 'STUDENT_APP'")
    List<Long> findUnitsByCourseUuid(@Param("courseUuid") UUID courseUuid);

    List<Unit> findAllBySchoolBookIdAndStatus(Long schoolBookId,UnitStatus status);
}