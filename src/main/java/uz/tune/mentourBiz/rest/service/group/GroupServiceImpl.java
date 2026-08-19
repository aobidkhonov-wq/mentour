package uz.tune.mentourBiz.rest.service.group;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.postExercise.UnitProgress;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.Course;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.CourseLesson;
import uz.tune.mentourBiz.rest.domain.schoolManagement.schedule.AttendanceRecord;
import uz.tune.mentourBiz.rest.domain.schoolManagement.schedule.GroupSchedule;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.Branch;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Level;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.*;
import uz.tune.mentourBiz.rest.payload.req.group.ReqGroupCreate;
import uz.tune.mentourBiz.rest.payload.req.group.ReqGroupUpdate;
import uz.tune.mentourBiz.rest.payload.req.schedule.ReqGroupSchedule;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.school.ResReferralLink;
import uz.tune.mentourBiz.rest.payload.res.school.group.ResGroup;
import uz.tune.mentourBiz.rest.payload.res.school.group.ResGroupBalance;
import uz.tune.mentourBiz.rest.payload.res.school.group.ResNextLesson;
import uz.tune.mentourBiz.rest.payload.res.school.group.enrollment.ResGroupStudent;
import uz.tune.mentourBiz.rest.repository.PaymentPackageRepo;
import uz.tune.mentourBiz.rest.repository.course.CourseLessonRepo;
import uz.tune.mentourBiz.rest.repository.course.CourseRepo;
import uz.tune.mentourBiz.rest.repository.group.GroupRepository;
import uz.tune.mentourBiz.rest.repository.group.LevelRepository;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.schedule.AttendanceRecordRepository;
import uz.tune.mentourBiz.rest.repository.schedule.GroupScheduleRepository;
import uz.tune.mentourBiz.rest.repository.school.BranchRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolMentorRepo;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.unit.UnitRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.UnitProgressRepository;
import uz.tune.mentourBiz.rest.repository.user.TeacherRepository;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.rest.service.lessons.CourseService;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;
    private final BranchRepository branchRepository;
    private final TeacherRepository teacherRepository;
    private final UserService userService;
    private final EnrollmentRepository enrollmentRepository;
    private final UserScopeService userScopeService;
    private final LevelRepository levelRepo;
    private final SchoolRepo schoolRepo;
    private final SchoolMentorRepo schoolMentorRepo;
    private final StudentRepo studentRepo;
    private final PaymentPackageRepo paymentPackageRepo;
    private final CourseLessonRepo courseLessonRepo;
    private final GroupScheduleRepository groupScheduleRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final UnitProgressRepository unitProgressRepository;
    private final AuthToViewEntity authToViewEntity;
    private final UnitRepository unitRepository;
    private final CourseRepo courseRepo;
    private final CourseService courseService;

    record HealthScoreData(Double aggregate, Double attendance, Double academic,
                           Double attendancePct, Double academicPct) {}

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    @Transactional
    public ResponseMessage createGroup(ReqGroupCreate request) {
        User currentUser = userService.getCurrentUser();

        UUID schoolId = userScopeService.resolveSchoolUuid(request.getSchoolId());

        Level level = levelRepo.findByUuid(request.getLevelId()).get();
        School school = schoolRepo.findByUuid(schoolId).get();
        Branch branch = branchRepository.findBySchool_Uuid(school.getUuid()).get();

        Teacher teacher = null;
        
        if(currentUser.getRole().equals(UserRole.TEACHER)) {
            teacher = teacherRepository.findByUser_Uuid(currentUser.getUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
        }
        else {
            teacher = teacherRepository.findByUser_Uuid(request.getTeacherId())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
        }
        


        Group group = new Group();
        group.setName(request.getName());
        group.setBranch(branch);
        group.setTeacher(teacher);
        group.setLevel(level);
        group.setGroupStatus(GroupStatus.ACTIVE);
        if (request.getLessonDays() != null) group.setLessonDays(request.getLessonDays());
        groupRepository.save(group);

        return new ResponseMessage("SUCCESS");
    }

    @Override
    @Transactional
    public ResponseMessage updateGroup(UUID groupId, ReqGroupUpdate request) {
        Group group = groupRepository.findByUuid(groupId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.GROUP_NOT_FOUND.getKey()));

        authToViewEntity.authorizeActionUponSchoolBroadAccess(group.getBranch().getSchool());

        if (request.getName() != null) group.setName(request.getName());

        if (request.getTeacherId() != null) {
            Teacher teacher = teacherRepository.findByUser_Uuid(request.getTeacherId())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
            authToViewEntity.authorizeActionUponTeacher(teacher);
            group.setTeacher(teacher);
        }

        if (request.getLevleUuid() != null) {
            Level level = levelRepo.findByUuid(request.getLevleUuid()).orElseThrow();
            group.setLevel(level);
        }

        if (request.getLessonDays() != null) group.setLessonDays(request.getLessonDays());

        groupRepository.save(group);
        return new ResponseMessage("Group updated successfully.");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResGroup> getGroups(Pageable pageable, UUID schoolId, UUID branchId, GroupStatus status,
                                    String className, String levelName, String teacherName) {
        User currentUser = userService.getCurrentUser();

        Collection<UUID> schoolUuids;

        UUID resolvedId = userScopeService.resolveSchoolUuid(schoolId);

        if (resolvedId != null) {
            // If a specific school is requested (and authorized), filter by it
            schoolUuids = List.of(resolvedId);
        } else {
            // Otherwise, fall back to all authorized schools for this user
            schoolUuids = userScopeService.getAuthorizedSchoolUuids();
        }

        UUID teacherUuidFiltered = (currentUser.getRole() == UserRole.TEACHER) ? currentUser.getUuid() : null;

        return groupRepository.findWithFilters(
                status,
                schoolUuids,
                branchId,
                teacherUuidFiltered,
                className,
                levelName,
                teacherName,
                pageable
        ).map(group -> {
            long count = enrollmentRepository.countByGroup_UuidAndStatusAndStudent_UserStatus(group.getUuid(), EnrollmentStatus.ONGOING, UserStatus.ACTIVE);
            ResGroup res = new ResGroup(group, count);

            HealthScoreData hs = calculateDetailedHealthScore(group.getUuid());
            res.setHealthScore(hs.aggregate());
            res.setAttendanceScore(hs.attendance());
            res.setAcademicScore(hs.academic());
            applyCardMetrics(res, group.getUuid(), hs);

            return res;
        });
    }

    private void applyCardMetrics(ResGroup res, UUID groupUuid, HealthScoreData hs) {
        res.setAttendancePercentage(hs.attendancePct());
        res.setAverageScorePercentage(hs.academicPct());

        List<LessonStatus> countableStatuses = List.of(
                LessonStatus.NEW, LessonStatus.STARTED, LessonStatus.FINISHED, LessonStatus.STUDENT_APP);
        res.setTotalLessons(courseLessonRepo.countByCourse_Group_UuidAndStatusIn(groupUuid, countableStatuses));

        res.setPaymentsDueCount(studentRepo.countOverdueByGroup(groupUuid));
        res.setPaymentsDueAmount(Math.abs(studentRepo.sumOverdueByGroup(groupUuid)));

        applyNextLesson(res, groupUuid);
    }

    private void applyNextLesson(ResGroup res, UUID groupUuid) {
        List<CourseLesson> upcomingLessons = courseLessonRepo.findAllByCourse_Group_UuidAndStatusIn(
                groupUuid, List.of(LessonStatus.NEW, LessonStatus.STARTED, LessonStatus.STUDENT_APP));
        Instant now = Instant.now();
        upcomingLessons.stream()
                .filter(l -> l.getStartTime() != null && l.getEndTime() != null && l.getEndTime().isAfter(now))
                .min(Comparator.comparing(CourseLesson::getStartTime))
                .ifPresent(l ->{
                    int utcOffset=5;
                    Group group = l.getCourse().getGroup();
                    Optional<Branch> byUuid = branchRepository.findByUuid(group.getBranch().getUuid());
                    if (byUuid.isPresent()) {
                        Branch branch = byUuid.get();
                        utcOffset=branch.getSchool().getUtcOffset();
                    }
                    res.setNextLesson(new ResNextLesson(l,utcOffset));
                });
    }
    @Override
    @Transactional
    public ResReferralLink generateReferralLink(UUID groupId) {
        Group group = getGroupAndAuthorize(groupId);
        School school = group.getBranch().getSchool();

        UUID referralCode = UUID.randomUUID();
        group.setReferralCode(referralCode);
        groupRepository.save(group);

        String baseUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;

        String link = baseUrl + "/classes/enroll?referralCode=" + referralCode + "&schoolId=" + school.getUuid() + "&groupId=" + group.getUuid();

        return new ResReferralLink(link);
    }

    private Group getGroupAndAuthorize(UUID groupId) {
        User currentUser = userService.getCurrentUser();

        Group group = groupRepository.findByUuid(groupId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.GROUP_NOT_FOUND.getKey()));

        if (currentUser.getRole().equals(UserRole.SYS_ADMIN)) {
            return group;
        }

        List<UUID> authorizedUuids = userScopeService.getAuthorizedSchoolUuids();

        if (!authorizedUuids.contains(group.getBranch().getSchool().getUuid())) {
            throw new PermissionForbidden(MessageKey.ORG_MISMATCH.getKey());
        }

        if (currentUser.getRole().equals(UserRole.TEACHER)) {
            if (group.getTeacher() == null || !group.getTeacher().getUser().getUuid().equals(currentUser.getUuid())) {
                throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
            }
        }

        return group;
    }

    @Override
    @Transactional
    public ResponseMessage createSchedule(ReqGroupSchedule request) {
        Group group = getGroupAndAuthorize(request.getGroupUuid());

        CourseLesson lesson = courseLessonRepo.findByUuid(request.getLessonUuid())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.LESSON_NOT_FOUND.getKey()));

        if (groupScheduleRepository.findByLesson(lesson).isPresent()) {
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

        groupScheduleRepository.save(schedule);
        createUnitsProgressesForNewSchedule(group.getUuid(), lesson.getUnits());

        // Initialize Attendance Records
        List<Enrollment> enrollments = enrollmentRepository.findAllByGroup_UuidAndStatus(group.getUuid(), EnrollmentStatus.ONGOING);
        // Dars yaratilganda yozuv allaqachon ochilgan bo'lishi mumkin — dublikat qo'shmaymiz.
        Set<Long> alreadyHasRecord = attendanceRecordRepository.findAllByLesson(lesson).stream()
                .map(a -> a.getStudent().getId())
                .collect(Collectors.toSet());
        List<AttendanceRecord> initialRecords = enrollments.stream()
                .filter(e -> alreadyHasRecord.add(e.getStudent().getId()))
                .map(e -> {
            AttendanceRecord ar = new AttendanceRecord();
            ar.setUuid(UUID.randomUUID());
            ar.setLesson(lesson);
            ar.setStudent(e.getStudent());
            ar.setStatus(AttendanceStatus.NOT_MARKED);
            ar.setIsMarked(false);
            return ar;
        }).collect(Collectors.toList());

        attendanceRecordRepository.saveAll(initialRecords);

        return new ResponseMessage("Schedule created and records initialized.");
    }

    @Transactional
    public void createUnitsProgressesForNewSchedule(UUID groupUuid, List<Unit> units) {
        if (units == null || units.isEmpty()) return;
        List<Enrollment> enrollments = enrollmentRepository.findAllByGroup_UuidAndStatus(groupUuid, EnrollmentStatus.ONGOING);
        if (enrollments.isEmpty()) return;

        for (Enrollment e : enrollments) {
            for (Unit u : units) {
                if (unitProgressRepository.findByStudentAndUnit(e.getStudent(), u).isEmpty()) {
                    UnitProgress up = new UnitProgress();
                    up.setStudent(e.getStudent());
                    up.setUnit(u);
                    up.setProgressPercentage(0);
                    up.setStatus(UnitProgressStatus.ATTEMPTED);
                    unitProgressRepository.save(up);
                }
            }
        }
    }


    private HealthScoreData calculateDetailedHealthScore(UUID groupUuid) {
        List<LessonStatus> activeStatuses = List.of(LessonStatus.STUDENT_APP, LessonStatus.FINISHED);
        List<CourseLesson> groupLessons = courseLessonRepo.findAllByCourse_Group_UuidAndStatusIn(groupUuid, activeStatuses);

        // 1. Attendance Component
        double attendancePct = 100.0;
        if (!groupLessons.isEmpty()) {
            List<AttendanceRecord> records = attendanceRecordRepository.findAllByLessonIn(groupLessons);
            if (!records.isEmpty()) {
                long marked = records.stream().filter(AttendanceRecord::getIsMarked).count();
                long attended = records.stream()
                        .filter(r -> r.getIsMarked() && (r.getStatus() == AttendanceStatus.PRESENT || r.getStatus() == AttendanceStatus.LATE))
                        .count();
                if (marked > 0) attendancePct = (attended * 100.0) / marked;
            }
        }
        double attendanceScore = Math.round((attendancePct / 100.0 * 5.0) * 10.0) / 10.0;

        // 2. Academic Component
        double academicPct = 0.0;
        UUID activeCourseUuid = courseRepo.findAllByGroupAndStatus(groupRepository.findByUuid(groupUuid).get(), CourseStatus.ACTIVE)
                .stream().findFirst().map(Course::getUuid).orElse(null);

        if (activeCourseUuid != null) {
            List<Enrollment> enrollments = enrollmentRepository.findAllByGroup_UuidAndStatus(groupUuid, EnrollmentStatus.ONGOING);
            List<Long> studentIds = enrollments.stream().map(e -> e.getStudent().getId()).toList();
            List<Long> unitIds = unitRepository.findUnitsByCourseUuid(activeCourseUuid);

            if (!studentIds.isEmpty() && unitIds != null && !unitIds.isEmpty()) {
                Double avg = unitProgressRepository.getAverageProgressForStudentsAndUnits(studentIds, unitIds);
                academicPct = (avg != null) ? avg : 0.0;
            }
        } else {
            academicPct = attendancePct; // Fallback if no units assigned yet
        }
        double academicScore = Math.round((academicPct / 100.0 * 5.0) * 10.0) / 10.0;

        // 3. Aggregate
        double aggregate = Math.round(((attendanceScore + academicScore) / 2.0) * 10.0) / 10.0;

        double attendancePctRounded = Math.round(attendancePct * 10.0) / 10.0;
        double academicPctRounded = Math.round(academicPct * 10.0) / 10.0;

        return new HealthScoreData(aggregate, attendanceScore, academicScore, attendancePctRounded, academicPctRounded);
    }


    @Override
    @Transactional
    public ResponseMessage deleteGroup(UUID groupId) {
        Group group = groupRepository.findByUuid(groupId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.GROUP_NOT_FOUND.getKey()));

        authToViewEntity.authorizeActionUponSchoolBroadAccess(group.getBranch().getSchool());

        User user = userService.getCurrentUser();
        if (user.getRole().equals(UserRole.TEACHER)) {
            if (group.getTeacher() == null || !group.getTeacher().getUser().getUuid().equals(user.getUuid())) {
                throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
            }
        }

        List<Enrollment> enrollments = enrollmentRepository.findAllByGroup_UuidAndStatus(groupId, EnrollmentStatus.ONGOING);
        for (Enrollment enrollment : enrollments) {
            enrollment.setStatus(EnrollmentStatus.DELETED);
            enrollment.setNote("Auto-deactivated due to Group deletion.");
        }
        enrollmentRepository.saveAll(enrollments);

        List<Course> activeCourses = courseRepo.findAllByGroupAndStatus(group, CourseStatus.ACTIVE);
        for (Course course : activeCourses) {
            courseService.deleteCourse(course.getUuid());
        }

        List<GroupSchedule> schedules = groupScheduleRepository.findAllByGroup_Uuid(groupId);
        for (GroupSchedule gs : schedules) {
            gs.setStatus(GroupScheduleStatus.DELETED);
        }
        groupScheduleRepository.saveAll(schedules);

        group.setGroupStatus(GroupStatus.DELETED);
        groupRepository.save(group);

        return new ResponseMessage("Group and all associated courses, lessons, and enrollments have been successfully deactivated.");
    }

    @Override
    @Transactional
    public ResponseMessage unDeleteGroup(UUID groupId) {
        Group group = groupRepository.findByUuid(groupId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.GROUP_NOT_FOUND.getKey()));

        if (group.getBranch().getStatus() == BranchStatus.DELETED ||
                group.getBranch().getSchool().getStatus() == SchoolStatus.DELETED) {
            throw new ValidationException(MessageKey.RESTORE_DELETED_BRANCH.getKey());
        }

        if (group.getGroupStatus() != GroupStatus.DELETED) {
            return new ResponseMessage("Group is already active.");
        }

        List<Enrollment> deletedEnrollments = enrollmentRepository
                .findAllByGroup_UuidAndStatus(groupId, EnrollmentStatus.DELETED);

        if (deletedEnrollments.isEmpty()) {
            group.setGroupStatus(GroupStatus.ACTIVE);
            groupRepository.save(group);
            return new ResponseMessage("Group restored. No enrollments were found to recover.");
        }

        List<UUID> studentUuids = deletedEnrollments.stream()
                .map(e -> e.getStudent().getUser().getUuid())
                .toList();

        List<EnrollmentStatus> activeStatuses = List.of(EnrollmentStatus.ONGOING, EnrollmentStatus.STARTED);

        Set<UUID> busyStudentUuids = enrollmentRepository
                .findAllByStudent_User_UuidInAndStatusIn(studentUuids, activeStatuses)
                .stream()
                .map(e -> e.getStudent().getUser().getUuid())
                .collect(Collectors.toSet());

        List<Enrollment> enrollmentsToRestore = new ArrayList<>();
        List<String> skippedNames = new ArrayList<>();

        for (Enrollment e : deletedEnrollments) {
            UUID sUuid = e.getStudent().getUser().getUuid();
            String fullName = e.getStudent().getUser().getFirstName() + " " + e.getStudent().getUser().getLastName();

            if (busyStudentUuids.contains(sUuid)) {
                skippedNames.add(fullName);
            } else {
                e.setStatus(EnrollmentStatus.ONGOING);
                enrollmentsToRestore.add(e);
            }
        }

        group.setGroupStatus(GroupStatus.ACTIVE);
        groupRepository.save(group);
        enrollmentRepository.saveAll(enrollmentsToRestore);

        StringBuilder response = new StringBuilder("Group '" + group.getName() + "' restored.");
        response.append("\nRestored students: ").append(enrollmentsToRestore.size());

        if (!skippedNames.isEmpty()) {
            response.append("\n\nSkipped ").append(skippedNames.size())
                    .append(" student(s) because they already have an active enrollment in another group: ")
                    .append(String.join(", ", skippedNames));
        }

        return new ResponseMessage(response.toString());
    }


    @Override
    @Transactional(readOnly = true)
    public List<ResGroup> getGroupsBySchool(UUID schoolId) {
        User currentUser = userService.getCurrentUser();

        // Verification Logic: Does the requested schoolId belong to the user's Org?
        if (!currentUser.getRole().equals(UserRole.SYS_ADMIN)) {
            List<UUID> ownedSchools = userScopeService.getDirectorSchoolUuids();
            if (schoolId != null && !ownedSchools.contains(schoolId)) {
                throw new PermissionForbidden(MessageKey.ORG_MISMATCH.getKey());
            }
        }

        // If no schoolId provided, use the single header or the full list
        Collection<UUID> targetIds = (schoolId != null) ? List.of(schoolId) : userScopeService.getAuthorizedSchoolUuids();

        List<Group> groups = groupRepository.findAllByBranch_School_UuidInAndGroupStatus(
                targetIds, Pageable.unpaged(), GroupStatus.ACTIVE).getContent();

        return groups.stream().map(group -> {
            long activeStudentCount = enrollmentRepository.countByGroup_UuidAndStatusAndStudent_UserStatus(
                    group.getUuid(), EnrollmentStatus.ONGOING, UserStatus.ACTIVE);
            return new ResGroup(group, activeStudentCount);
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResGroupBalance> getGroupBalancesBySchool(UUID schoolId, String groupName, String teacherName, Boolean onlyOverdue) {
        User currentUser = userService.getCurrentUser();

        // Verify the requested school belongs to the user's Org.
        if (!(currentUser.getRole().equals(UserRole.SYS_ADMIN)||currentUser.getRole().equals(UserRole.SCHOOL_ADMIN))) {
            List<UUID> ownedSchools = userScopeService.getDirectorSchoolUuids();
            if (schoolId != null && !ownedSchools.contains(schoolId)) {
                throw new PermissionForbidden(MessageKey.ORG_MISMATCH.getKey());
            }
        }

        Collection<UUID> targetIds = (schoolId != null) ? List.of(schoolId) : userScopeService.getAuthorizedSchoolUuids();

        List<Group> groups = groupRepository.findActiveBySchoolWithFilters(targetIds, groupName, teacherName);

        return groups.stream().map(group -> {
            long allStudentBalance = studentRepo.sumBalanceByGroup(group.getUuid());
            long overdueStudentsBalance = Math.abs(studentRepo.sumOverdueByGroup(group.getUuid()));
            long overdueStudentsCount = studentRepo.countOverdueByGroup(group.getUuid());
            return new ResGroupBalance(group, allStudentBalance, overdueStudentsBalance, overdueStudentsCount);
        }).filter(res -> !Boolean.TRUE.equals(onlyOverdue) || res.getOverdueStudentsBalance() > 0).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResGroupStudent> getGroupStudents(Integer page, Integer size, UUID groupUuid, String studentName,
                                                  EnrollmentStatus status, Boolean onlyOverdue, UUID packageId) {
        User currentUser = userService.getCurrentUser();

        // SYS_ADMIN sees all schools; others are limited to their authorized schools.
        Collection<UUID> schoolUuids = currentUser.getRole().equals(UserRole.SYS_ADMIN)
                ? null
                : userScopeService.getAuthorizedSchoolUuids();

        Pageable pageable = Pageable.ofSize(size != null ? size : 1000).withPage(page != null ? page : 0);

        return enrollmentRepository.findGroupStudentsFiltered(
                groupUuid,
                schoolUuids,
                status,
                Boolean.TRUE.equals(onlyOverdue),
                packageId,
                studentName,
                pageable
        ).map(ResGroupStudent::new);
    }

    @Override
    @Transactional(readOnly = true)
    public ResGroup getGroup(UUID groupId) {
        User currentUser = userService.getCurrentUser();
        Group group = groupRepository.findByUuid(groupId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.GROUP_NOT_FOUND.getKey()));

        authToViewEntity.authorizeActionUponSchoolBroadAccess(group.getBranch().getSchool());

        if (currentUser.getRole().equals(UserRole.TEACHER)) {
            if (group.getTeacher() == null || !group.getTeacher().getUser().getUuid().equals(currentUser.getUuid())) {
                throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
            }
        }

        if (currentUser.getRole().equals(UserRole.STUDENT)) {
            Student student = studentRepo.findByUser(currentUser)
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

            boolean isEnrolled = enrollmentRepository.existsByStudentAndGroup(student, group);
            if (!isEnrolled) {
                throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
            }
        }

        long activeStudentCount = enrollmentRepository.countByGroup_UuidAndStatusAndStudent_UserStatus(groupId, EnrollmentStatus.ONGOING, UserStatus.ACTIVE);
        ResGroup res = new ResGroup(group, activeStudentCount);
        HealthScoreData hs = calculateDetailedHealthScore(groupId);
        res.setHealthScore(hs.aggregate());
        res.setAttendanceScore(hs.attendance());
        res.setAcademicScore(hs.academic());
        applyCardMetrics(res, groupId, hs);
        return res;
    }

}