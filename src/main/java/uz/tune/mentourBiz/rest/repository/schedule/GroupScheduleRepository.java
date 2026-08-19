package uz.tune.mentourBiz.rest.repository.schedule;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.Course;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.CourseLesson;
import uz.tune.mentourBiz.rest.domain.schoolManagement.schedule.GroupSchedule;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.enums.GroupScheduleStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupScheduleRepository extends BaseRepository<GroupSchedule> {
    Page<GroupSchedule> findAllByStatusAndGroup_Uuid(GroupScheduleStatus status, UUID groupId, Pageable pageable);
    List<GroupSchedule> findAllByGroup_Uuid(UUID groupId);

    Optional<GroupSchedule> findByUuid(UUID uuid);

    List<GroupSchedule> findAllByGroup_UuidAndStatusOrderByDueDateAsc(UUID groupId, GroupScheduleStatus status);

    List<GroupSchedule> findAllByLesson(CourseLesson lesson);

    Optional<GroupSchedule> findByLesson(CourseLesson lesson);

    @EntityGraph(attributePaths = {"unit"})
    @Query("SELECT gs FROM GroupSchedule gs JOIN gs.unit u WHERE gs.group.uuid = :uuid AND gs.status = :status ORDER BY u.sortOrder ASC, gs.dueDate ASC")
    List<GroupSchedule> findAllByGroup_UuidAndStatusOrderByUnitSortOrderAscDueDateAsc(UUID uuid, GroupScheduleStatus status);

    @Query("SELECT MAX(gs.dueDate) FROM GroupSchedule gs WHERE gs.group.branch.school.uuid = :schoolUuid AND gs.status <> 'DELETED'")
    java.time.Instant findLatestDueDateBySchool(@Param("schoolUuid") java.util.UUID schoolUuid);

    Optional<GroupSchedule> findByGroupAndUnit(Group group, Unit unit);
    @Modifying
    @Query("UPDATE GroupSchedule gs SET gs.status = 'DELETED' " +
            "WHERE gs.group = :group AND gs.lesson.course = :course " +
            "AND gs.status = 'ACTIVE'")
    void deactivateSchedulesForGroupAndCourse(@Param("group") Group group, @Param("course") Course course);

    @Query("""
    SELECT gs FROM GroupSchedule gs 
    JOIN gs.lesson l 
    JOIN l.units u 
    WHERE gs.group.uuid = :groupUuid 
      AND u.uuid = :unitUuid 
      AND gs.status = :status
""")
    Optional<GroupSchedule> findByGroupUuidAndLessonContainingUnit(
            @Param("groupUuid") UUID groupUuid,
            @Param("unitUuid") UUID unitUuid,
            @Param("status") GroupScheduleStatus status
    );

}