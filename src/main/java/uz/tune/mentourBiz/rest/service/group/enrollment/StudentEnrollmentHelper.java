package uz.tune.mentourBiz.rest.service.group.enrollment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.postExercise.UnitProgress;
import uz.tune.mentourBiz.rest.domain.schoolManagement.schedule.GroupSchedule;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.enums.BillingPlanType;
import uz.tune.mentourBiz.rest.enums.EnrollmentStatus;
import uz.tune.mentourBiz.rest.enums.GroupScheduleStatus;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.enums.UnitProgressStatus;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentLessonConsumptionRepository;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.schedule.GroupScheduleRepository;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.unit.exercise.UnitProgressRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Low-level, repository-only helper shared by user creation, student transfer and enrollment flows.
 * It deliberately depends on no other service so it can be reused without introducing circular
 * dependencies. Authorization stays the responsibility of the calling service.
 */
@Service
@RequiredArgsConstructor
public class StudentEnrollmentHelper {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepo studentRepo;
    private final GroupScheduleRepository groupScheduleRepository;
    private final UnitProgressRepository unitProgressRepository;
    private final EnrollmentLessonConsumptionRepository consumptionRepository;

    /**
     * All ongoing enrollments of the student, oldest first (deterministic order).
     * Multi-group aware replacement for the various {@code findTop...ONGOING} lookups.
     */
    public List<Enrollment> getOngoingEnrollments(UUID userUuid) {
        return enrollmentRepository.findAllByStudent_User_UuidAndStatus(userUuid, EnrollmentStatus.ONGOING)
                .stream()
                .sorted(Comparator.comparing(e -> e.getCreatedAt() != null ? e.getCreatedAt() : Instant.EPOCH))
                .collect(Collectors.toList());
    }

    /**
     * Resolves the ongoing enrollment a request targets for group-scoped reads (lesson feed, ranking).
     * <ul>
     *   <li>{@code requestedGroupUuid} given → must be one of the student's ongoing groups (else 403).</li>
     *   <li>{@code null} → the primary (oldest) ongoing enrollment, keeping single-group clients working.</li>
     * </ul>
     */
    public Enrollment resolveActiveEnrollment(Student student, UUID requestedGroupUuid) {
        List<Enrollment> ongoing = getOngoingEnrollments(student.getUser().getUuid());
        if (ongoing.isEmpty()) {
            throw new EntityNotFoundException(MessageKey.GROUP_NO_ENROLLMENT.getKey());
        }
        if (requestedGroupUuid != null) {
            return ongoing.stream()
                    .filter(e -> e.getGroup().getUuid().equals(requestedGroupUuid))
                    .findFirst()
                    .orElseThrow(() -> new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey()));
        }
        return ongoing.get(0);
    }

    /**
     * Resolves the ongoing enrollment whose group actually schedules the given unit. This lets
     * unit-specific operations (open unit, exam access, unit summary) work for multi-group students
     * without the client having to send the group explicitly. Falls back to the primary enrollment.
     */
    public Enrollment resolveEnrollmentForUnit(Student student, UUID unitUuid) {
        List<Enrollment> ongoing = getOngoingEnrollments(student.getUser().getUuid());
        if (ongoing.isEmpty()) {
            throw new EntityNotFoundException(MessageKey.GROUP_NO_ENROLLMENT.getKey());
        }
        return ongoing.stream()
                .filter(e -> groupScheduleRepository.findByGroupUuidAndLessonContainingUnit(
                        e.getGroup().getUuid(), unitUuid, GroupScheduleStatus.ACTIVE).isPresent())
                .findFirst()
                .orElse(ongoing.get(0));
    }

    /**
     * Additively enrolls a student into a group WITHOUT closing existing ongoing enrollments,
     * enabling membership in several groups at once. Idempotent: a no-op if already enrolled.
     */
    @Transactional
    public void enrollToGroup(Student student, Group targetGroup) {
        boolean alreadyEnrolled = enrollmentRepository
                .findByStudentAndGroupAndStatus(student, targetGroup, EnrollmentStatus.ONGOING)
                .isPresent();
        if (alreadyEnrolled) return;

        Enrollment newEnrollment = new Enrollment();
        newEnrollment.setStudent(student);
        newEnrollment.setGroup(targetGroup);
        newEnrollment.setStatus(EnrollmentStatus.ONGOING);
        enrollmentRepository.save(newEnrollment);

        createAndUnlockProgressForPastUnits(student, targetGroup);
    }

    /**
     * Closes the student's current ongoing enrollment(s), moves the student to the target
     * group's branch/school, creates a fresh ongoing enrollment and unlocks past-due units.
     *
     * @param withBillingPlan when {@code true}, the billing plan / package of the source enrollment
     *                        is carried over to the new enrollment in the target group; otherwise the
     *                        student joins the target group without a billing plan.
     */
    @Transactional
    public void transferToGroup(Student student, Group targetGroup, boolean withBillingPlan) {
        List<Enrollment> currentEnrollments = enrollmentRepository
                .findAllByStudent_User_UuidAndStatus(student.getUser().getUuid(), EnrollmentStatus.ONGOING);

        // Capture the source billing plan before the enrollments are closed.
        Enrollment sourceEnrollment = withBillingPlan
                ? currentEnrollments.stream()
                        .filter(e -> e.getBillingPlan() != null)
                        .findFirst()
                        .orElse(null)
                : null;

        // The student may already be enrolled in the target group (e.g. transferring into the same
        // group, or a multi-group member). Reuse that enrollment instead of inserting a second
        // ONGOING row, which would violate the uk_student_group_ongoing partial unique index.
        Enrollment existingTargetEnrollment = currentEnrollments.stream()
                .filter(e -> e.getGroup().getUuid().equals(targetGroup.getUuid()))
                .findFirst()
                .orElse(null);

        // Close every ongoing enrollment except the one we keep in the target group.
        List<Enrollment> toClose = currentEnrollments.stream()
                .filter(e -> e != existingTargetEnrollment)
                .collect(Collectors.toList());
        toClose.forEach(e -> e.setStatus(EnrollmentStatus.FINISHED));
        enrollmentRepository.saveAll(toClose);

        // Student moves to the new branch/school
        student.setSchool(targetGroup.getBranch().getSchool());
        studentRepo.save(student);

        Enrollment newEnrollment = existingTargetEnrollment != null ? existingTargetEnrollment : new Enrollment();
        newEnrollment.setStudent(student);
        newEnrollment.setGroup(targetGroup);
        newEnrollment.setStatus(EnrollmentStatus.ONGOING);
        // Only carry the plan over from a different (closed) enrollment; never copy onto itself.
        if (sourceEnrollment != null && sourceEnrollment != newEnrollment) {
            copyBillingPlan(sourceEnrollment, newEnrollment);
        }
        enrollmentRepository.save(newEnrollment);

        createAndUnlockProgressForPastUnits(student, targetGroup);
    }

    /**
     * Copies the billing plan / package fields from a source enrollment onto a target enrollment.
     * For LESSON_PACK plans the remaining (unused) lessons are carried over rather than the raw total.
     * Shared by the transfer flow and the user-update group-sync flow.
     */
    public void copyBillingPlan(Enrollment source, Enrollment target) {
        target.setBillingPlan(source.getBillingPlan());
        target.setBillingType(source.getBillingType());
        target.setLessonsTotal(carryOverLessonsTotal(source));
        target.setPaidUntil(source.getPaidUntil());
        target.setLastPaidAt(source.getLastPaidAt());
        target.setAmountPaid(source.getAmountPaid());
    }

    /**
     * Removes the billing plan / package fields from an enrollment, mirroring {@link #copyBillingPlan}.
     * Used when reactivating a student who should start without their previous plan.
     */
    public void clearBillingPlan(Enrollment enrollment) {
        enrollment.setBillingPlan(null);
        enrollment.setBillingType(null);
        enrollment.setLessonsTotal(null);
        enrollment.setPaidUntil(null);
        enrollment.setLastPaidAt(null);
        enrollment.setAmountPaid(0L);
    }

    /**
     * Lesson quota to seed the new enrollment with. For LESSON_PACK the remaining lessons must be
     * preserved exactly: the new enrollment starts with zero consumption rows, so its total is set to
     * the source's remaining (bought total minus non-excused consumption), never below zero. For any
     * other billing type the original total is copied verbatim.
     */
    private Integer carryOverLessonsTotal(Enrollment sourceEnrollment) {
        Integer lessonsTotal = sourceEnrollment.getLessonsTotal();
        if (sourceEnrollment.getBillingType() != BillingPlanType.LESSON_PACK || lessonsTotal == null) {
            return lessonsTotal;
        }
        long consumed = consumptionRepository.countByEnrollmentAndExcusedFalse(sourceEnrollment);
        return (int) Math.max(0, lessonsTotal - consumed);
    }

    public void createAndUnlockProgressForPastUnits(Student student, Group group) {
        // Find all schedules for the new group
        List<GroupSchedule> allSchedules = groupScheduleRepository.findAllByGroup_Uuid(group.getUuid());

        if (allSchedules.isEmpty()) return;

        List<Unit> allUnitsToInitialize = allSchedules.stream()
                .map(GroupSchedule::getLesson)
                .flatMap(lesson -> lesson.getUnits().stream())
                .distinct().toList();

        Instant now = Instant.now();
        // Identify units that happened in the past
        Set<UUID> pastUnitUuids = allSchedules.stream()
                .filter(s -> s.getDueDate() != null && s.getDueDate().isBefore(now))
                .map(GroupSchedule::getLesson)
                .flatMap(l -> l.getUnits().stream())
                .map(Unit::getUuid).collect(Collectors.toSet());

        // Batch fetch existing progress to avoid N+1
        Map<UUID, UnitProgress> existingMap = unitProgressRepository
                .findAllByUnit_UuidInAndStudent_User_Uuid(
                        allUnitsToInitialize.stream().map(Unit::getUuid).collect(Collectors.toSet()),
                        student.getUser().getUuid()
                ).stream().collect(Collectors.toMap(p -> p.getUnit().getUuid(), p -> p));

        List<UnitProgress> toSave = new ArrayList<>();
        for (Unit unit : allUnitsToInitialize) {
            UnitProgress p = existingMap.get(unit.getUuid());
            if (p == null) {
                p = new UnitProgress();
                p.setStudent(student);
                p.setUnit(unit);
                p.setProgressPercentage(0);
                p.setStatus(UnitProgressStatus.ATTEMPTED);
            }

            // Cross-school logic: If student is new to this school's curriculum,
            // unlock all units that are already overdue.
            if (pastUnitUuids.contains(unit.getUuid())) {
                p.setManuallyUnlocked(true);
                p.setIsVideoWatched(true);
            }
            toSave.add(p);
        }
        unitProgressRepository.saveAll(toSave);
    }
}
