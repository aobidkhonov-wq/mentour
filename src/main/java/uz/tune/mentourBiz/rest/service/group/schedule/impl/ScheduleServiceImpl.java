package uz.tune.mentourBiz.rest.service.group.schedule.impl;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.Organization;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.postExercise.UnitProgress;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.CourseLesson;
import uz.tune.mentourBiz.rest.domain.schoolManagement.schedule.AttendanceRecord;
import uz.tune.mentourBiz.rest.domain.schoolManagement.schedule.GroupSchedule;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.BillingPlan;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.EnrollmentLessonConsumption;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.*;
import uz.tune.mentourBiz.rest.payload.req.schedule.ReqGroupSchedule;
import uz.tune.mentourBiz.rest.payload.req.schedule.ReqMarkAttendance;
import uz.tune.mentourBiz.rest.payload.req.schedule.ReqScheduleUpdate;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.school.group.schedule.ResGroupSchedule;
import uz.tune.mentourBiz.rest.payload.res.school.group.schedule.attendance.ResAttendanceRecord;
import uz.tune.mentourBiz.rest.repository.SchoolDirectorRepo;
import uz.tune.mentourBiz.rest.repository.course.CourseLessonRepo;
import uz.tune.mentourBiz.rest.repository.group.GroupRepository;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentLessonConsumptionRepository;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.schedule.AttendanceRecordRepository;
import uz.tune.mentourBiz.rest.repository.schedule.GroupScheduleRepository;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.unit.UnitRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.UnitProgressRepository;
import uz.tune.mentourBiz.rest.repository.user.TeacherRepository;
import uz.tune.mentourBiz.rest.service.group.enrollment.EnrollmentBillingService;
import uz.tune.mentourBiz.rest.service.group.schedule.ScheduleService;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final GroupScheduleRepository scheduleRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final GroupRepository groupRepository;
    private final UnitRepository unitRepository;
    private final StudentRepo studentRepository;
    private final TeacherRepository teacherRepository;
    private final UserService userService;
    private final UserScopeService userScopeService;
    private final EnrollmentRepository enrollmentRepository;
    private final UnitProgressRepository unitProgressRepository;
    private final GroupScheduleRepository groupScheduleRepository;
    private final CourseLessonRepo courseLessonRepo;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final SchoolDirectorRepo schoolDirectorRepo;
    private final EnrollmentLessonConsumptionRepository consumptionRepository;
    private final EnrollmentBillingService enrollmentBillingService;
    @Override
    @Transactional
    public ResponseMessage createSchedule(ReqGroupSchedule request) {
        Group group = getGroupAndAuthorize(request.getGroupUuid());
        CourseLesson lesson = courseLessonRepo.findByUuid(request.getLessonUuid())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.LESSON_NOT_FOUND.getKey()));

        Optional<GroupSchedule> existingSchedule = scheduleRepository.findByLesson(lesson);
        if (existingSchedule.isPresent()) {
            throw new ValidationException(MessageKey.UNIT_ALREADY_SCHEDULED.getKey());
        }

        GroupSchedule schedule = new GroupSchedule();
        schedule.setGroup(group);
        schedule.setLesson(lesson);
        schedule.setDueDate(request.getDueDate() != null ? request.getDueDate() : lesson.getEndTime());
        schedule.setStatus(GroupScheduleStatus.ACTIVE);

        if (!lesson.getUnits().isEmpty()) {
            schedule.setUnit(lesson.getUnits().get(0));
        }

        scheduleRepository.save(schedule);

        createUnitsProgressesForNewSchedule(group.getUuid(), lesson.getUnits());

        List<Enrollment> enrollments = enrollmentRepository.findAllByGroup_UuidAndStatus(group.getUuid(), EnrollmentStatus.ONGOING);
        // Dars yaratilganda yozuv allaqachon ochilgan bo'lishi mumkin — dublikat qo'shmaymiz.
        Set<Long> alreadyHasRecord = attendanceRecordRepository.findAllByLesson(lesson).stream()
                .map(a -> a.getStudent().getId())
                .collect(Collectors.toSet());
        List<AttendanceRecord> initialRecords = new ArrayList<>();

        for (Enrollment e : enrollments) {
            if (!alreadyHasRecord.add(e.getStudent().getId())) continue;
            AttendanceRecord ar = new AttendanceRecord();
            ar.setUuid(UUID.randomUUID());
            ar.setLesson(lesson);
            ar.setStudent(e.getStudent());
            ar.setStatus(AttendanceStatus.NOT_MARKED);
            ar.setIsMarked(false);
            ar.setIsAttendanceNotified(false);
            ar.setIsUnitNotified(false);
            initialRecords.add(ar);
        }
        attendanceRecordRepository.saveAll(initialRecords);

        return new ResponseMessage("Schedule created and records initialized.");
    }




    @Override
    @Transactional
    public void createUnitsProgressesForNewSchedule(UUID groupUuid, List<Unit> units) {
        if (units == null || units.isEmpty()) return;

        List<Enrollment> enrollments = enrollmentRepository.findAllByGroup_UuidAndStatus(groupUuid, EnrollmentStatus.ONGOING);
        if (enrollments.isEmpty()) return;

        List<Long> studentIds = enrollments.stream().map(e -> e.getStudent().getId()).toList();
        List<Long> unitIds = units.stream().map(Unit::getId).toList();

        Set<String> existingKeys = unitProgressRepository
                .findAllByStudent_IdInAndUnit_IdIn(studentIds, unitIds)
                .stream()
                .map(p -> p.getStudent().getId() + "-" + p.getUnit().getId())
                .collect(Collectors.toSet());

        List<UnitProgress> progressToSave = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            for (Unit unit : units) {
                String key = enrollment.getStudent().getId() + "-" + unit.getId();
                if (!existingKeys.contains(key)) {
                    UnitProgress up = new UnitProgress();
                    up.setStudent(enrollment.getStudent());
                    up.setUnit(unit);
                    up.setProgressPercentage(0);
                    up.setStatus(UnitProgressStatus.ATTEMPTED);
                    progressToSave.add(up);
                }
            }
        }
        unitProgressRepository.saveAll(progressToSave);
    }

    @Override
    @Transactional
    public ResponseMessage updateSchedule(ReqScheduleUpdate request) {
        GroupSchedule schedule = scheduleRepository.findByUuid(request.getScheduleUuid())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHEDULE_NOT_FOUND.getKey()));

        getGroupAndAuthorize(schedule.getGroup().getUuid());

        if (request.getDueDate() != null) {
            schedule.setDueDate(request.getDueDate());
        }

        if (request.getUnitUuid() != null) {
            Unit newUnit = unitRepository.findByUuid(request.getUnitUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.UNIT_NOT_FOUND.getKey()));

            schedule.setUnit(newUnit);
        }

        scheduleRepository.save(schedule);


        CourseLesson linkedLesson = schedule.getLesson();
        if (linkedLesson.getUnits() != null && !linkedLesson.getUnits().isEmpty()) {
            createUnitsProgressesForNewSchedule(schedule.getGroup().getUuid(), linkedLesson.getUnits());
        }

        return new ResponseMessage("Schedule updated successfully and student progress initialized for all lesson units.");
    }
    @Override
    @Transactional
    public ResponseMessage deleteSchedule(UUID scheduleUuid) {

        GroupSchedule schedule = scheduleRepository.findByUuid(scheduleUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHEDULE_NOT_FOUND.getKey()));

        getGroupAndAuthorize(schedule.getGroup().getUuid());

        Instant now = Instant.now();
        if(now.isAfter(schedule.getDueDate())) {
            return new ResponseMessage("Can't be deleted after the lesson has passed.");
        }

        List<AttendanceRecord> attendanceRecords = null;
        attendanceRepository.deleteAll(attendanceRecords);

        scheduleRepository.delete(schedule);
        return new ResponseMessage("Schedule and related attendance records deleted");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResGroupSchedule> getSchedulesForGroup(UUID groupId, Pageable pageable) {
        getGroupAndAuthorize(groupId);
        return scheduleRepository.findAllByStatusAndGroup_Uuid(GroupScheduleStatus.ACTIVE,groupId,pageable).map(ResGroupSchedule::new);
    }

    @Override
    @Transactional
    public ResponseMessage markSingleAttendance(ReqMarkAttendance request) {
        CourseLesson lesson = courseLessonRepo.findByUuid(request.getLessonUuid()).orElseThrow();

        // Optionally record who actually conducted the lesson (a substitute), so payroll credits this
        // lesson to them rather than the group's default teacher.
        if (request.getConductedByTeacherUuid() != null) {
            lesson.setConductedByTeacher(teacherRepository.findByUser_Uuid(request.getConductedByTeacherUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.TEACHER_NOT_FOUND.getKey())));
            courseLessonRepo.save(lesson);
        }

        List<Student> student = studentRepository.findAllByUser_UuidIn(request.getStudentUuids());
        List<AttendanceRecord> attendanceRecords = new ArrayList<>();

        student.forEach(s -> {
            // Eskidan qolgan dublikat yozuvlar bo'lsa, hammasini bir xil holatga keltiramiz —
            // shunda o'qishda qaysi biri tanlanishidan qat'i nazar natija bir xil bo'ladi.
            List<AttendanceRecord> existing = attendanceRecordRepository.findAllByLessonAndStudent(lesson, s);
            if (existing.isEmpty()) {
                existing = List.of(new AttendanceRecord());
            }
            for (AttendanceRecord attendance : existing) {
                attendance.setLesson(lesson);
                attendance.setStudent(s);
                attendance.setStatus(request.getStatus());
                attendance.setNote(request.getNote());
                attendance.setMarkedBy(userService.getCurrentUser());
                attendance.setIsMarked(true);
                attendanceRecords.add(attendance);
            }
        });
        attendanceRepository.saveAll(attendanceRecords);

        // A conducted lesson bills every eligible enrollment in the group, whether or not the student
        // attended (excused absences are refunded/restored by an admin later):
        //   LESSON_PACK -> charge the plan price for this lesson.
        //   MONTHLY with a lesson cap -> count it toward the cap and expire when the cap is reached.
        consumeLessonForPacks(lesson);

        return new ResponseMessage("Marked attendance records successfully");
    }

    @Override
    @Transactional
    public ResponseMessage assignLessonTeacher(UUID lessonUuid, UUID teacherUuid) {
        CourseLesson lesson = courseLessonRepo.findByUuid(lessonUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.LESSON_NOT_FOUND.getKey()));

        // Null teacherUuid clears the substitute, reverting the lesson to the group's default teacher.
        if (teacherUuid == null) {
            lesson.setConductedByTeacher(null);
        } else {
            lesson.setConductedByTeacher(teacherRepository.findByUser_Uuid(teacherUuid)
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.TEACHER_NOT_FOUND.getKey())));
        }
        courseLessonRepo.save(lesson);
        return new ResponseMessage("Lesson teacher updated.");
    }

    private void consumeLessonForPacks(CourseLesson lesson) {
        if (lesson.getCourse() == null || lesson.getCourse().getGroup() == null) return;
        UUID groupUuid = lesson.getCourse().getGroup().getUuid();
        User markedBy = userService.getCurrentUser();

        List<Enrollment> enrollments = enrollmentRepository.findAllByGroup_UuidAndStatus(
                groupUuid, EnrollmentStatus.ONGOING);

        for (Enrollment enrollment : enrollments) {
            BillingPlan plan = enrollment.getBillingPlan();
            if (plan == null) continue;

            // Idempotent: one consumption row per (enrollment, lesson), even if attendance is re-marked.
            if (consumptionRepository.existsByEnrollmentAndLesson(enrollment, lesson)) continue;

            if (enrollment.getBillingType() == BillingPlanType.LESSON_PACK) {
                // Pay-per-lesson: record the lesson and charge the plan price against the som balance.
                // The consumption row lets an admin later excuse the lesson, which refunds this charge.
                EnrollmentLessonConsumption consumption = new EnrollmentLessonConsumption();
                consumption.setEnrollment(enrollment);
                consumption.setLesson(lesson);
                consumptionRepository.save(consumption);

                String note = "Per-lesson charge: " + enrollment.getGroup().getName() + " (" + plan.getName() + ")";
                enrollmentBillingService.chargeBillingPlan(enrollment.getStudent(), plan, enrollment.getGroup(), markedBy, note);

            } else if (enrollment.getBillingType() == BillingPlanType.FIXED_MONTHLY
                    && enrollment.getLessonsTotal() != null) {
                // Monthly plan with a lesson cap (e.g. 12 lessons / month): count toward the cap.
                EnrollmentLessonConsumption consumption = new EnrollmentLessonConsumption();
                consumption.setEnrollment(enrollment);
                consumption.setLesson(lesson);
                consumptionRepository.save(consumption);

                long consumed = consumptionRepository.countByEnrollmentAndExcusedFalse(enrollment);
                if (consumed >= enrollment.getLessonsTotal()) {
                    enrollment.setStatus(EnrollmentStatus.EXPIRED);
                    enrollmentRepository.save(enrollment);
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResAttendanceRecord> getAttendanceForSchedule(UUID lessonUuid) {
        CourseLesson courseLesson = courseLessonRepo.findByUuid(lessonUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHEDULE_NOT_FOUND.getKey()));
        getGroupAndAuthorize(courseLesson.getCourse().getGroup().getUuid());

        return attendanceRepository.findAllByLessonIn(List.of(courseLesson)).stream()
                .map(ResAttendanceRecord::new)
                .collect(Collectors.toList());
    }

    private Group getGroupAndAuthorize(UUID groupId) {
        User currentUser = userService.getCurrentUser();
        Group group = groupRepository.findByUuid(groupId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.GROUP_NOT_FOUND.getKey()));

        if (currentUser.getRole().equals(UserRole.SYS_ADMIN)) return group;

        if (currentUser.getRole().equals(UserRole.SCHOOL_DIRECTOR)) {
            Organization org = schoolDirectorRepo.findByUser(currentUser).get().getOrganization();
            if (group.getBranch().getSchool().getOrganization().equals(org)) return group;
            throw new PermissionForbidden(MessageKey.ORG_MISMATCH.getKey());
        }

        UUID schoolUuid = userScopeService.getCurrentUserSchoolUuid();
        if (!group.getBranch().getSchool().getUuid().equals(schoolUuid)) {
            throw new PermissionForbidden(MessageKey.ORG_MISMATCH.getKey());
        }

        if (currentUser.getRole().equals(UserRole.TEACHER)) {
            if (group.getTeacher() == null || !group.getTeacher().getUser().getUuid().equals(currentUser.getUuid())) {
                throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
            }
        }
        return group;
    }
}