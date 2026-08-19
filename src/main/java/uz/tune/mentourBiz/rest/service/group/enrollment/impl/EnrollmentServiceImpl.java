package uz.tune.mentourBiz.rest.service.group.enrollment.impl;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.Course;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.BillingPlan;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.EnrollmentLessonConsumption;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.BillingPlanType;
import uz.tune.mentourBiz.rest.enums.CourseStatus;
import uz.tune.mentourBiz.rest.enums.EnrollmentStatus;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.payload.req.enrollment.ReqAssignBillingPlan;
import uz.tune.mentourBiz.rest.payload.req.enrollment.ReqEnrollmentCreate;
import uz.tune.mentourBiz.rest.payload.req.enrollment.ReqEnrollmentStatusUpdate;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.school.group.enrollment.ResEnrollment;
import uz.tune.mentourBiz.rest.repository.course.CourseRepo;
import uz.tune.mentourBiz.rest.repository.group.BillingPlanRepository;
import uz.tune.mentourBiz.rest.repository.group.GroupRepository;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentLessonConsumptionRepository;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.service.FinanceService;
import uz.tune.mentourBiz.rest.service.group.enrollment.EnrollmentBillingService;
import uz.tune.mentourBiz.rest.service.group.enrollment.EnrollmentService;
import uz.tune.mentourBiz.rest.service.group.enrollment.StudentEnrollmentHelper;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

    private static final ZoneId UZ_ZONE = ZoneId.of("Asia/Tashkent");

    private final EnrollmentRepository enrollmentRepository;
    private final GroupRepository groupRepository;
    private final StudentRepo studentRepository;
    private final UserService userService;
    private final UserScopeService userScopeService;
    private final StudentRepo studentRepo;
    private final FinanceService financeService;
    private final CourseRepo courseRepo;
    private final AuthToViewEntity authToViewEntity;
    private final StudentEnrollmentHelper studentEnrollmentHelper;
    private final BillingPlanRepository billingPlanRepository;
    private final EnrollmentBillingService enrollmentBillingService;
    private final EnrollmentLessonConsumptionRepository consumptionRepository;

    @Override
    @Transactional
    public ResponseMessage createEnrollments(ReqEnrollmentCreate request) {
        User currentUser = userService.getCurrentUser();
        Group targetGroup = groupRepository.findByUuid(request.getGroupId())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.GROUP_NOT_FOUND.getKey()));


        if (!currentUser.getRole().equals(UserRole.SYS_ADMIN)) {
            List<UUID> authorizedUuids = userScopeService.getAuthorizedSchoolUuids();
            if (!authorizedUuids.contains(targetGroup.getBranch().getSchool().getUuid())) {
                throw new PermissionForbidden(MessageKey.ORG_MISMATCH.getKey());
            }
        }

        // Billing plan (12 lessons / monthly / ...) is a global catalog item the student picks at
        // enrollment time. Its price is charged against the student's som balance (may go into debt).
        BillingPlan billingPlan = null;
        if (CoreUtils.isPresent(request.getBillingPlanId())) {
            billingPlan = billingPlanRepository.findByUuid(request.getBillingPlanId())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.BILLING_PLAN_NOT_FOUND.getKey()));
        }

        List<Student> studentsToEnroll = studentRepo.findAllByUuidIn(request.getStudentIds());
        Set<UUID> alreadyEnrolledUuids = enrollmentRepository.findExistingStudentUuidsInGroupAndStatus(
                targetGroup.getUuid(), request.getStudentIds(), EnrollmentStatus.ONGOING);

        List<Course> groupCourses = courseRepo.findAllByGroupAndStatus(targetGroup, CourseStatus.ACTIVE);

       if (billingPlan != null && groupCourses.isEmpty()) {
            throw new ValidationException(MessageKey.GROUP_NO_ACTIVE_COURSE.getKey());
        }

        List<Enrollment> enrollmentsToSave = new ArrayList<>();

        for (Student student : studentsToEnroll) {
            if (alreadyEnrolledUuids.contains(student.getUuid())) continue;

            long charged = 0L;
            if (billingPlan != null && billingPlan.getType() == BillingPlanType.FIXED_MONTHLY) {
                String note = "Group enrollment: " + targetGroup.getName() + " (" + billingPlan.getName() + ")";
                charged = enrollmentBillingService.chargeBillingPlan(student, billingPlan, targetGroup, currentUser, note);
            }

             Enrollment newEnrollment = new Enrollment();
            newEnrollment.setStudent(student);
            newEnrollment.setGroup(targetGroup);
            newEnrollment.setStatus(EnrollmentStatus.ONGOING);
            applyBillingPlan(newEnrollment, billingPlan, LocalDate.now(UZ_ZONE), true, charged);
            enrollmentsToSave.add(newEnrollment);

            studentEnrollmentHelper.createAndUnlockProgressForPastUnits(student, targetGroup);
            // Old PaymentPackage per-course enrollment charge removed: billing is now driven solely by
            // the enrollment's BillingPlan (charged above for FIXED_MONTHLY). Keeping the old charge here
            // double-billed students who had both a course package and a billing plan.
        }

        enrollmentRepository.saveAll(enrollmentsToSave);

        return new ResponseMessage("Enrollment process completed. Enrolled: " + enrollmentsToSave.size() + ".");
    }

    /**
     * Anchors the billing period on {@code startDate}: one period runs from startDate to
     * startDate + countMonth months (same day-of-month), and every later renewal falls on that same
     * day. When {@code charged} is false the plan starts in the future and nothing has been paid yet,
     * so {@code paidUntil} is set to startDate itself and the renewal scheduler takes the first
     * charge on that day.
     *
     * @param chargedAmount what the student was actually billed — the plan price less any discount.
     */
    private void applyBillingPlan(Enrollment enrollment, BillingPlan plan, LocalDate startDate,
                                  boolean charged, long chargedAmount) {
        if (plan == null) return;
        enrollment.setBillingPlan(plan);
        enrollment.setBillingType(plan.getType());
        enrollment.setLastPaidAt(charged ? startDate.atStartOfDay(UZ_ZONE).toInstant() : null);

        if (plan.getType() == BillingPlanType.LESSON_PACK) {
            enrollment.setLessonsTotal(null);
            enrollment.setPaidUntil(null);
            enrollment.setAmountPaid(0L);
        } else if (plan.getType() == BillingPlanType.FIXED_MONTHLY) {
            if (charged) {
                int months = plan.getCountMonth() != null ? plan.getCountMonth() : 1;
                enrollment.setPaidUntil(startDate.plusMonths(months).atStartOfDay(UZ_ZONE).toInstant());
                enrollment.setLessonsTotal(plan.getLessonCount());
                enrollment.setAmountPaid(chargedAmount);
            } else {
                enrollment.setPaidUntil(startDate.atStartOfDay(UZ_ZONE).toInstant());
                enrollment.setLessonsTotal(null);
                enrollment.setAmountPaid(0L);
            }
        }
    }

    @Override
    @Transactional
    public ResponseMessage assignBillingPlan(UUID groupUuid, ReqAssignBillingPlan request) {
        User currentUser = userService.getCurrentUser();
        Group targetGroup = groupRepository.findByUuid(groupUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.GROUP_NOT_FOUND.getKey()));

        if (!currentUser.getRole().equals(UserRole.SYS_ADMIN)) {
            List<UUID> authorizedUuids = userScopeService.getAuthorizedSchoolUuids();
            if (!authorizedUuids.contains(targetGroup.getBranch().getSchool().getUuid())) {
                throw new PermissionForbidden(MessageKey.ORG_MISMATCH.getKey());
            }
        }

        BillingPlan billingPlan = billingPlanRepository.findByUuid(request.getBillingPlanId())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.BILLING_PLAN_NOT_FOUND.getKey()));

        if (courseRepo.findAllByGroupAndStatus(targetGroup, CourseStatus.ACTIVE).isEmpty()) {
            throw new ValidationException(MessageKey.GROUP_NO_ACTIVE_COURSE.getKey());
        }

        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now(UZ_ZONE);
        boolean chargeNow = !startDate.isAfter(LocalDate.now(UZ_ZONE));
        boolean replaceExisting = Boolean.TRUE.equals(request.getReplaceExistingPlan());

        List<Student> students = studentRepo.findAllByUuidIn(request.getStudentUuids());

        int assigned = 0;
        int keptExisting = 0;
        for (Student student : students) {
            Optional<Enrollment> existing = enrollmentRepository
                    .findByStudentAndGroupAndStatus(student, targetGroup, EnrollmentStatus.ONGOING);

            Enrollment enrollment;
            if (existing.isPresent()) {
                enrollment = existing.get();
              if (enrollment.getBillingPlan() != null && !replaceExisting) {
                    keptExisting++;
                    continue;
                }
            } else {
                enrollment = new Enrollment();
                enrollment.setStudent(student);
                enrollment.setGroup(targetGroup);
                enrollment.setStatus(EnrollmentStatus.ONGOING);
                studentEnrollmentHelper.createAndUnlockProgressForPastUnits(student, targetGroup);
            }

            // Charge on the start date itself: today (or a past date) bills right away, a future
            // start date is left to the renewal scheduler, which charges on that exact day.
            long charged = 0L;
            if (billingPlan.getType() == BillingPlanType.FIXED_MONTHLY && chargeNow) {
                String note = "Package assignment: " + targetGroup.getName() + " (" + billingPlan.getName() + ")";
                charged = enrollmentBillingService.chargeBillingPlan(student, billingPlan, targetGroup, currentUser, note);
            }

            applyBillingPlan(enrollment, billingPlan, startDate, chargeNow, charged);
            enrollmentRepository.save(enrollment);
            assigned++;
        }

        return new ResponseMessage(
                "Package assignment completed. Assigned: " + assigned + ", kept existing: " + keptExisting + ".");
    }

    @Override
    @Transactional
    public ResponseMessage restoreLesson(UUID enrollmentId, UUID lessonId) {
        Enrollment enrollment = enrollmentRepository.findByUuid(enrollmentId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.GROUP_NO_ENROLLMENT.getKey()));

        authToViewEntity.authorizeActionUponSchoolBroadAccess(enrollment.getGroup().getBranch().getSchool());

        EnrollmentLessonConsumption consumption = consumptionRepository
                .findByEnrollment_UuidAndLesson_Uuid(enrollmentId, lessonId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.LESSON_NOT_FOUND.getKey()));

        if (consumption.isExcused()) {
            return new ResponseMessage("Lesson already restored.");
        }

        consumption.setExcused(true);
        consumptionRepository.save(consumption);

        BillingPlan plan = enrollment.getBillingPlan();

        // LESSON_PACK is billed per lesson: excusing the lesson refunds the price it was charged.
        if (enrollment.getBillingType() == BillingPlanType.LESSON_PACK && plan != null) {
            String note = "Per-lesson refund (excused): " + enrollment.getGroup().getName() + " (" + plan.getName() + ")";
            enrollmentBillingService.refundBillingPlan(enrollment.getStudent(), plan, enrollment.getGroup(), userService.getCurrentUser(), note);
        }

         if (enrollment.getStatus() == EnrollmentStatus.EXPIRED && enrollment.getLessonsTotal() != null) {
            long consumed = consumptionRepository.countByEnrollmentAndExcusedFalse(enrollment);
            if (consumed < enrollment.getLessonsTotal()) {
                enrollment.setStatus(EnrollmentStatus.ONGOING);
                enrollmentRepository.save(enrollment);
            }
        }

        return new ResponseMessage("Lesson restored to the student.");
    }

    @Override
    @Transactional
    public ResponseMessage updateEnrollmentStatus(UUID enrollmentId, ReqEnrollmentStatusUpdate request) {
        Enrollment enrollment = enrollmentRepository.findByUuid(enrollmentId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.GROUP_NO_ENROLLMENT.getKey()));

        authToViewEntity.authorizeActionUponSchoolBroadAccess(enrollment.getGroup().getBranch().getSchool());

        enrollment.setStatus(request.getStatus());
        enrollmentRepository.save(enrollment);
        return new ResponseMessage("Enrollment status updated.");
    }

    private void deactivateCurrentEnrollment(Student student) {
        enrollmentRepository.findTopByStudent_User_UuidAndStatus(student.getUser().getUuid(), EnrollmentStatus.ONGOING)
                .ifPresent(oldEnrollment -> {
                    oldEnrollment.setStatus(EnrollmentStatus.FINISHED);
                    enrollmentRepository.save(oldEnrollment);
                    // todo handle unit progress maybe
                });
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ResEnrollment> getEnrollments(Pageable pageable, UUID groupId, UUID studentId) {
        User currentUser = userService.getCurrentUser();

        if (currentUser.getRole().equals(UserRole.SYS_ADMIN)) {
            if (CoreUtils.isPresent(groupId)) return enrollmentRepository.findAllByGroup_Uuid(groupId, pageable).map(ResEnrollment::new);
            if (CoreUtils.isPresent(studentId)) return enrollmentRepository.findAllByStudent_User_Uuid(studentId, pageable).map(ResEnrollment::new);
            return enrollmentRepository.findAll(pageable).map(ResEnrollment::new);
        }

        Collection<UUID> authorizedUuids = userScopeService.getAuthorizedSchoolUuids();

        if (CoreUtils.isPresent(groupId)) {
            return enrollmentRepository.findAllByGroup_UuidAndGroup_Branch_School_UuidIn(groupId, authorizedUuids, pageable).map(ResEnrollment::new);
        } else if (CoreUtils.isPresent(studentId)) {
            return enrollmentRepository.findAllByStudent_User_UuidAndGroup_Branch_School_UuidIn(studentId, authorizedUuids, pageable).map(ResEnrollment::new);
        }

        return enrollmentRepository.findAllByGroup_Branch_School_UuidIn(authorizedUuids, pageable).map(ResEnrollment::new);
    }
}