package uz.tune.mentourBiz.rest.service.user.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.config.GlobalVar;
import uz.tune.mentourBiz.config.logger.Logger;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.Organization;
import uz.tune.mentourBiz.rest.domain.Region;
import uz.tune.mentourBiz.rest.domain.SchoolAcademicConfig;
import uz.tune.mentourBiz.rest.domain.SchoolDirector;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.postExercise.UnitProgress;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.Course;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.CourseLesson;
import uz.tune.mentourBiz.rest.domain.schoolManagement.schedule.AttendanceRecord;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.domain.userManagement.user.SchoolAdmin;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.*;
import uz.tune.mentourBiz.rest.payload.req.student.ReqReferralRegister;
import uz.tune.mentourBiz.rest.payload.req.user.ReqAdminResetPassword;
import uz.tune.mentourBiz.rest.payload.req.user.ReqUserCreate;
import uz.tune.mentourBiz.rest.payload.req.user.ReqUserDeleteList;
import uz.tune.mentourBiz.rest.payload.req.user.ReqUserFreezeList;
import uz.tune.mentourBiz.rest.payload.req.user.ReqUserUpdateDetails;
import uz.tune.mentourBiz.rest.payload.res.ResParentDetail;
import uz.tune.mentourBiz.rest.payload.res.ResRegion;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.school.ResSchoolInfo;
import uz.tune.mentourBiz.rest.payload.res.school.group.ResGroup;
import uz.tune.mentourBiz.rest.payload.res.user.ResUserInfo;
import uz.tune.mentourBiz.rest.repository.OrganizationRepository;
import uz.tune.mentourBiz.rest.repository.SchoolAcademicConfigRepo;
import uz.tune.mentourBiz.rest.repository.SchoolDirectorRepo;
import uz.tune.mentourBiz.rest.repository.StudentParentContactRepository;
import uz.tune.mentourBiz.rest.repository.course.CourseLessonRepo;
import uz.tune.mentourBiz.rest.repository.course.CourseRepo;
import uz.tune.mentourBiz.rest.repository.group.GroupRepository;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.schedule.AttendanceRecordRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.unit.exercise.UnitProgressRepository;
import uz.tune.mentourBiz.rest.repository.user.SchoolAdminRepo;
import uz.tune.mentourBiz.rest.repository.user.TeacherRepository;
import uz.tune.mentourBiz.rest.repository.user.UserRepo;
import uz.tune.mentourBiz.rest.service.SubscriptionValidator;
import uz.tune.mentourBiz.rest.service.group.enrollment.StudentEnrollmentHelper;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.rest.service.permission.PermissionService;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepo userRepo;
    private final SchoolRepo schoolRepo;
    private final SchoolAdminRepo schoolAdminRepo;
    private final StudentRepo studentRepo;
    private final PasswordEncoder passwordEncoder;
    private final UserScopeService userScopeService;
    private final PermissionService permissionService;
    private final GroupRepository groupRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TeacherRepository teacherRepository;
    private final SchoolAcademicConfigRepo schoolAcademicConfigRepo;
    private final SchoolDirectorRepo schoolDirectorRepo;
    private final OrganizationRepository organizationRepository;
    private final AuthToViewEntity authToViewEntity;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final CourseLessonRepo courseLessonRepo;
    private final CourseRepo courseRepo;
    private final UnitProgressRepository unitProgressRepository;
    private final SubscriptionValidator subscriptionValidator;
    private final StudentEnrollmentHelper studentEnrollmentHelper;
    private final StudentParentContactRepository parentContactRepository;

    @Override
    public boolean calculateIsAtRisk(Student student, SchoolAcademicConfig config) {
        Enrollment enrollment = enrollmentRepository
                .findTopByStudent_User_UuidAndStatus(student.getUser().getUuid(), EnrollmentStatus.ONGOING)
                .orElse(null);
        if (enrollment == null || enrollment.getGroup() == null) {
            return false;
        }

        StudentGroupStats stats = computeStudentGroupStats(student, enrollment.getGroup());
        double attendancePercentage = (stats.markedLessons() == 0) ? 0.0
                : Math.round(stats.attendedLessons() * 10000.0 / stats.markedLessons()) / 100.0;
        int overallScore = (stats.scoreCount() > 0)
                ? (int) Math.round((double) stats.scoreSum() / stats.scoreCount()) : 0;

        // --- At-risk qarori ---
        // Ma'lumot umuman bo'lmasa (yangi student) o'sha mezon bo'yicha at-risk deb belgilanmaydi.
        Integer attendanceThreshold = config.getAttendanceThreshold();
        Integer resultsThreshold = config.getResultsThreshold();

        boolean attendanceAtRisk = attendanceThreshold != null && stats.markedLessons() > 0
                && attendancePercentage < attendanceThreshold;
        boolean scoreAtRisk = resultsThreshold != null && stats.scoreCount() > 0
                && overallScore < resultsThreshold;

        return scoreAtRisk || attendanceAtRisk;
    }

    /**
     * Raw per-group numbers shared by the at-risk calculation and the student profile stats.
     * heldLessons/totalLessons: darslardan nechtasi o'tib bo'lgan / kursdagi jami darslar.
     */
    private record StudentGroupStats(long markedLessons, long attendedLessons,
                                     int scoreSum, int scoreCount,
                                     long heldLessons, long totalLessons) {}

    private StudentGroupStats computeStudentGroupStats(Student student, Group group) {
        ZoneId zoneId = ZoneId.of("Asia/Tashkent");
        LocalDate today = LocalDate.now(zoneId);
        Instant now = Instant.now();

        // --- Attendance % : /api/v1/attendance (getOverallAttendance) bilan bir xil ---
        List<AttendanceRecord> records = attendanceRecordRepository
                .findActiveForStudentInGroup(group.getUuid(), student.getUuid(), now);
        long attended = records.stream()
                .filter(r -> r.getStatus() == AttendanceStatus.PRESENT || r.getStatus() == AttendanceStatus.LATE)
                .count();

        // --- Overall score % : /api/v1/marks/course (overallScore) bilan bir xil ---
        int sumForAverage = 0;
        int countForAverage = 0;
        long heldLessons = 0;
        long totalLessons = 0;
        for (Course course : courseRepo.findAllByGroupAndStatus(group, CourseStatus.ACTIVE)) {
            List<CourseLesson> lessons = courseLessonRepo.findWithFilters(
                    null, List.of(LessonStatus.STUDENT_APP, LessonStatus.FINISHED), null, null,
                    List.of(course.getUuid()), null, null, Pageable.unpaged()).getContent();
            totalLessons += lessons.size();

            List<Long> unitIds = lessons.stream()
                    .flatMap(l -> l.getUnits().stream())
                    .distinct()
                    .map(Unit::getId)
                    .toList();
            Map<String, Integer> progressMap = unitIds.isEmpty() ? Map.of() :
                    unitProgressRepository.findAllByStudent_IdInAndUnit_IdIn(List.of(student.getId()), unitIds)
                            .stream().collect(Collectors.toMap(
                                    p -> p.getStudent().getId() + ":" + p.getUnit().getId(),
                                    UnitProgress::getProgressPercentage));

            for (CourseLesson lesson : lessons) {
                LocalDate date = LocalDate.ofInstant(lesson.getStartTime(), zoneId);
                if (date.isAfter(today)) continue;
                heldLessons++;
                for (Unit unit : lesson.getUnits()) {
                    if (!unit.getStatus().equals(UnitStatus.ACTIVE)) continue;
                    Integer progress = progressMap.getOrDefault(student.getId() + ":" + unit.getId(), 0);
                    sumForAverage += (progress != null ? progress : 0);
                    countForAverage++;
                }
            }
        }
        return new StudentGroupStats(records.size(), attended, sumForAverage, countForAverage, heldLessons, totalLessons);
    }

    @Override
    public ResUserInfo getUserInfo() {
        User user = this.getCurrentUser();

        ResSchoolInfo resSchoolInfo = null;
        ResGroup resGroup = null;
        School schoolEntity = null;
        Student student = null;
        SchoolAcademicConfig schoolAcademicConfig = null;

        if (user.getRole().equals(UserRole.STUDENT)) {
            student = studentRepo.findByUser(user).orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
            schoolEntity = student.getSchool();
            resSchoolInfo = new ResSchoolInfo(schoolEntity);
            schoolAcademicConfig = schoolAcademicConfigRepo.findBySchool_Uuid(schoolEntity.getUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));

            student.setIsAtRisk(calculateIsAtRisk(student, schoolAcademicConfig));
            studentRepo.save(student);
            // Multi-group: primary group kept in `resGroup`; full list populated below.
            List<Enrollment> ongoingEnrollments = studentEnrollmentHelper.getOngoingEnrollments(user.getUuid());
            if (!ongoingEnrollments.isEmpty()) {
                resGroup = new ResGroup(ongoingEnrollments.get(0).getGroup());
            }
        } else if (user.getRole().equals(UserRole.TEACHER)) {
            var teacher = teacherRepository.findByUser(user).orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
            schoolEntity = teacher.getSchool();
            resSchoolInfo = new ResSchoolInfo(schoolEntity);
        } else if (user.getRole().equals(UserRole.SCHOOL_ADMIN)) {
            var admin = schoolAdminRepo.findByUser(user).orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
            schoolEntity = admin.getSchool();
            resSchoolInfo = new ResSchoolInfo(schoolEntity);
        } else if (user.getRole().equals(UserRole.SCHOOL_DIRECTOR)) {
            SchoolDirector director = schoolDirectorRepo.findByUser(user).orElseThrow(() -> new EntityNotFoundException(MessageKey.DIRECTOR_NOT_FOUND.getKey()));
            resSchoolInfo = new ResSchoolInfo();
            resSchoolInfo.setName("Director: " + director.getOrganization().getName());
        }

        ResUserInfo info = new ResUserInfo(user, resSchoolInfo, resGroup, schoolEntity);
        if (student != null) {
            info.setStudentUuid(student.getUuid());
            info.setCoins(student.getCoins());
            info.setScore(student.getScore());
            info.setBalance(student.getCurrentBalance());
            info.setAtRisk(student.getIsAtRisk());
            info.setGroupList(studentEnrollmentHelper.getOngoingEnrollments(user.getUuid())
                    .stream().map(ResGroup::new).collect(Collectors.toList()));
        }

        if (user.getRole().equals(UserRole.SCHOOL_DIRECTOR)) {
            schoolDirectorRepo.findByUser(user).ifPresent(d -> {
                if (d.getOrganization() != null) {
                    info.setOrganizationUuid(d.getOrganization().getUuid());
                    info.setOrganizationName(d.getOrganization().getName());
                    info.setLinkedToOrganization(true);
                    Region region = d.getOrganization().getSchools().get(0).getRegion();
                    info.getSchool().setRegionDetails(new ResRegion(region));

                }
            });
        }
        return info;
    }

    @Override
    public User getCurrentUser() {
        return GlobalVar.getUserDetails().getUser();
    }

    @Override
    @Transactional
    public ResUserInfo getUserByUuid(UUID userId) {
        User user = userRepo.findByUuid(userId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
        //fixme getschool fix
        ResSchoolInfo schoolInfo = userScopeService.getSchoolInfoForUser(user);
        ResUserInfo info = new ResUserInfo(user, schoolInfo);

        if (user.getRole().equals(UserRole.STUDENT)) {
            studentRepo.findByUser(user).ifPresent(student -> fillStudentProfile(info, student));
        } else if (user.getRole().equals(UserRole.TEACHER)) {
            info.setTotalStudents(enrollmentRepository.countTotalStudentsByTeacher(user.getUuid()));
            info.setActiveClasses(groupRepository.countByTeacher_User_UuidAndGroupStatus(user.getUuid(), GroupStatus.ACTIVE));
        }
        return info;
    }

    /**
     * Student profile page (single user view) uchun qo'shimcha ma'lumotlar:
     * group list, attendance %, average score %, o'tilgan/jami darslar, rank va parent kontaktlar.
     */
    private void fillStudentProfile(ResUserInfo info, Student student) {
        info.setStudentUuid(student.getUuid());
        info.setCoins(student.getCoins());
        info.setScore(student.getScore());
        info.setBalance(student.getCurrentBalance());

        List<Enrollment> ongoing = studentEnrollmentHelper.getOngoingEnrollments(student.getUser().getUuid());
        info.setGroupList(ongoing.stream().map(ResGroup::new).collect(Collectors.toList()));
        if (!ongoing.isEmpty()) {
            info.setGroups(new ResGroup(ongoing.get(0).getGroup()));
        }

        long marked = 0, attended = 0, held = 0, totalLessons = 0;
        int scoreSum = 0, scoreCount = 0;
        for (Enrollment enrollment : ongoing) {
            StudentGroupStats stats = computeStudentGroupStats(student, enrollment.getGroup());
            marked += stats.markedLessons();
            attended += stats.attendedLessons();
            held += stats.heldLessons();
            totalLessons += stats.totalLessons();
            scoreSum += stats.scoreSum();
            scoreCount += stats.scoreCount();
        }
        info.setAttendancePercentage(marked > 0 ? (int) Math.round(attended * 100.0 / marked) : 0);
        info.setAverageScorePercentage(scoreCount > 0 ? (int) Math.round((double) scoreSum / scoreCount) : 0);
        info.setCompletedLessons(held);
        info.setTotalLessons(totalLessons);

        School school = student.getSchool();
        if (school != null) {
            int score = student.getScore() != null ? student.getScore() : 0;
            info.setRank(studentRepo.countStudentsWithHigherScoreInSchool(school.getUuid(), score) + 1);
            info.setTotalStudentsInSchool(studentRepo.countActiveStudentsIn(List.of(school.getUuid())));

            schoolAcademicConfigRepo.findBySchool_Uuid(school.getUuid()).ifPresent(config -> {
                student.setIsAtRisk(calculateIsAtRisk(student, config));
                studentRepo.save(student);
            });
        }
        info.setAtRisk(Boolean.TRUE.equals(student.getIsAtRisk()));

        info.setParentContacts(parentContactRepository.findAllByStudentAndIsActiveTrue(student).stream()
                .map(ResParentDetail::new)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public Page<ResUserInfo> getAllUsers(UserStatus status, UserRole role, String fullName,
                                         String username, String schoolName, Pageable pageable,
                                         String schoolUuid, UUID classId, UUID courseId) {
        User currentUser = this.getCurrentUser();

        UUID inputUuid = (schoolUuid != null) ? UUID.fromString(schoolUuid) : null;
        UUID resolvedId = userScopeService.resolveSchoolUuid(inputUuid);

        Collection<UUID> scopeSchoolUuids;
        if (resolvedId != null) {
            scopeSchoolUuids = List.of(resolvedId);
        } else {
            scopeSchoolUuids = userScopeService.getAuthorizedSchoolUuids();
        }

        UUID targetTeacherUuid = (currentUser.getRole().equals(UserRole.TEACHER)) ? currentUser.getUuid() : null;
        UserStatus filterStatus = (status == null) ? UserStatus.ACTIVE : status;

        Page<User> usersPage = userRepo.findAllWithFilters(
                filterStatus,
                role,
                fullName,
                username,
                schoolName,
                scopeSchoolUuids,
                currentUser.getId(),
                classId,
                courseId,
                targetTeacherUuid,
                pageable
        );

        List<User> userList = usersPage.getContent();
        if (userList.isEmpty()) return usersPage.map(u -> new ResUserInfo(u, null));

        List<UUID> userUuids = userList.stream().map(User::getUuid).toList();

        Map<UUID, Student> studentMap = studentRepo.findByUserInWithClasses(userList).stream()
                .collect(Collectors.toMap(s -> s.getUser().getUuid(), s -> s));
        Map<UUID, Teacher> teacherMap = teacherRepository.findAllByUser_UuidIn(userUuids).stream()
                .collect(Collectors.toMap(t -> t.getUser().getUuid(), t -> t));
        Map<UUID, SchoolAdmin> adminMap = schoolAdminRepo.findAllByUserUuidIn(userUuids).stream()
                .collect(Collectors.toMap(a -> a.getUser().getUuid(), a -> a));

        return usersPage.map(userResp -> {
            UUID uuid = userResp.getUuid();
            School schoolEntity = null;
            ResGroup groupInfo = null;
            List<ResGroup> ongoingGroups = List.of();
            Student s = null;
            Teacher teacher = null;
            long totalStudentsCount = 0;
            long activeClassesCount = 0;

            if (studentMap.containsKey(uuid)) {
                s = studentMap.get(uuid);
                schoolEntity = s.getSchool();
                if (s.getEnrollments() != null) {
                    ongoingGroups = s.getEnrollments().stream()
                            .filter(e -> e.getStatus().equals(EnrollmentStatus.ONGOING))
                            .sorted(Comparator.comparing(e -> e.getCreatedAt() != null ? e.getCreatedAt() : Instant.EPOCH))
                            .map(e -> new ResGroup(e.getGroup()))
                            .toList();
                    groupInfo = ongoingGroups.isEmpty() ? null : ongoingGroups.get(0);
                }
            } else if (teacherMap.containsKey(uuid)) {
                teacher = teacherMap.get(uuid);
                schoolEntity = teacher.getSchool();
                totalStudentsCount = enrollmentRepository.countTotalStudentsByTeacher(uuid);
                activeClassesCount = groupRepository.countByTeacher_User_UuidAndGroupStatus(uuid, GroupStatus.ACTIVE);
            } else if (adminMap.containsKey(uuid)) {
                schoolEntity = adminMap.get(uuid).getSchool();
            }
            ResSchoolInfo schoolInfo = (schoolEntity != null) ? new ResSchoolInfo(schoolEntity) : null;
            ResUserInfo dto = new ResUserInfo(userResp, schoolInfo, groupInfo, schoolEntity, s, teacher, totalStudentsCount, activeClassesCount);
            dto.setGroupList(ongoingGroups);
            dto.setPhoneNumber(userResp.getPhoneNumber());
            dto.setAddress(userResp.getAddress());

            if (s != null) {

                SchoolAcademicConfig schoolAcademicConfig = schoolAcademicConfigRepo.findBySchool_Uuid(schoolEntity.getUuid())
                        .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));

                s.setIsAtRisk(calculateIsAtRisk(s, schoolAcademicConfig));
                studentRepo.saveAndFlush(s);
                dto.setStudentUuid(s.getUuid());
                dto.setAtRisk(s.getIsAtRisk());
            }

            return dto;
        });
    }


    @Override
    @Transactional
    public ResponseMessage updateUser(UUID userId, ReqUserUpdateDetails request) {
        User userToUpdate = userRepo.findByUuid(userId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
        User currentUser = this.getCurrentUser();

        if (!currentUser.getRole().equals(UserRole.SYS_ADMIN)) {
            List<UUID> authorizedUuids = userScopeService.getAuthorizedSchoolUuids();
            ResSchoolInfo targetInfo = userScopeService.getSchoolInfoForUser(userToUpdate);

            // If it's a student/teacher, they must belong to one of the Director's schools
            if (targetInfo != null && !authorizedUuids.contains(targetInfo.getUuid())) {
                throw new PermissionForbidden(MessageKey.ORG_MISMATCH.getKey());
            }

            // Directors cannot edit other Directors
            if (userToUpdate.getRole().equals(UserRole.SCHOOL_DIRECTOR)) {
                throw new PermissionForbidden(MessageKey.DIRECTOR_RESTRICTION.getKey());
            }
        }

        if (userToUpdate.getRole().equals(UserRole.SCHOOL_DIRECTOR) && request.getOrganizationUuid() != null) {
            if (!currentUser.getRole().equals(UserRole.SYS_ADMIN)) {
                throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
            }
            SchoolDirector sd = schoolDirectorRepo.findByUser(userToUpdate)
                    .orElseGet(() -> {
                        SchoolDirector newSd = new SchoolDirector();
                        newSd.setUser(userToUpdate);
                        return newSd;
                    });
            Organization org = organizationRepository.findByUuid(request.getOrganizationUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.ORG_NOT_FOUND.getKey()));
            sd.setOrganization(org);
            schoolDirectorRepo.save(sd);
        }

        userToUpdate.setFirstName(request.getFirstName());
        userToUpdate.setLastName(request.getLastName());
        if (CoreUtils.isPresent(request.getPassword())) {
            userToUpdate.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getStatus() != null) {
            // Re-activating a student increases the school's active student count,
            // so enforce the school limit just like when adding a student.
            if (userToUpdate.getRole().equals(UserRole.STUDENT)
                    && request.getStatus().equals(UserStatus.ACTIVE)
                    && !userToUpdate.getStatus().equals(UserStatus.ACTIVE)) {
                Student student = studentRepo.findByUser(userToUpdate)
                        .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
                subscriptionValidator.validateStudentAddition(student.getSchool(), 1);
            }
            userToUpdate.setStatus(request.getStatus());
        }

        if (CoreUtils.isPresent(request.getUsername()) && !userToUpdate.getUsername().equals(request.getUsername())) {
            if (userRepo.existsByUsername(request.getUsername())) {
                throw new ValidationException(MessageKey.USERNAME_TAKEN.getKey());
            }
            userToUpdate.setUsername(request.getUsername());
        }
        if (CoreUtils.isPresent(request.getPhoneNumber())) {
            userToUpdate.setPhoneNumber(request.getPhoneNumber());
        }
        if (CoreUtils.isPresent(request.getAddress())) {
            userToUpdate.setAddress(request.getAddress());
        }

        if (request.getGroupUuids() != null && userToUpdate.getRole().equals(UserRole.STUDENT)) {
            Student student = studentRepo.findByUser(userToUpdate)
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
            addStudentToGroups(student, request.getGroupUuids(), Boolean.TRUE.equals(request.getWithBillingPlan()));
        }

        if (userToUpdate.getRole().equals(UserRole.STUDENT)
                && (UserStatus.FROZEN.equals(request.getStatus()) || UserStatus.ACTIVE.equals(request.getStatus()))
                && Boolean.FALSE.equals(request.getWithBillingPlan())) {
            Student student = studentRepo.findByUser(userToUpdate)
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
            clearBillingPlansForStudent(student);
        }

        userRepo.save(userToUpdate);
        return new ResponseMessage("User details updated successfully.");
    }
    
    private void clearBillingPlansForStudent(Student student) {
        List<Enrollment> ongoingEnrollments = enrollmentRepository
                .findAllByStudent_User_UuidAndStatus(student.getUser().getUuid(), EnrollmentStatus.ONGOING);
        ongoingEnrollments.forEach(studentEnrollmentHelper::clearBillingPlan);
        enrollmentRepository.saveAll(ongoingEnrollments);
    }


    private void addStudentToGroups(Student student, List<UUID> groupUuids, boolean withBillingPlan) {
        List<Enrollment> ongoingEnrollments = enrollmentRepository
                .findAllByStudent_User_UuidAndStatus(student.getUser().getUuid(), EnrollmentStatus.ONGOING);

        Set<UUID> requestedGroupUuids = new HashSet<>(groupUuids);

        Enrollment sourceEnrollment = withBillingPlan
                ? ongoingEnrollments.stream()
                        .filter(e -> e.getBillingPlan() != null)
                        .findFirst()
                        .orElse(null)
                : null;

        Set<UUID> enrolledGroupUuids = ongoingEnrollments.stream()
                .map(e -> e.getGroup().getUuid())
                .collect(Collectors.toSet());

        for (UUID groupUuid : requestedGroupUuids) {
            if (enrolledGroupUuids.contains(groupUuid)) {
                continue;
            }
            Group group = groupRepository.findByUuid(groupUuid)
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.GROUP_NOT_FOUND.getKey()));
            authToViewEntity.authorizeActionUponSchoolBroadAccess(group.getBranch().getSchool());

            Enrollment enrollment = new Enrollment();
            enrollment.setStudent(student);
            enrollment.setGroup(group);
            enrollment.setStatus(EnrollmentStatus.ONGOING);
            if (sourceEnrollment != null) {
                studentEnrollmentHelper.copyBillingPlan(sourceEnrollment, enrollment);
            }
            enrollmentRepository.save(enrollment);

            studentEnrollmentHelper.createAndUnlockProgressForPastUnits(student, group);
        }
    }

    @Override
    @Transactional
    public ResponseMessage resetPassword(UUID userId, ReqAdminResetPassword request) {
        User user = userRepo.findByUuid(userId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepo.save(user);
        return new ResponseMessage("User password has been reset successfully.");
    }

    @Override
    @Transactional
    public User createUserAccount(String firstName, String lastName, String username, String password, UserRole role, String phoneNumber) {
        if (userRepo.existsByUsername(username)) {
            throw new ValidationException(MessageKey.USERNAME_TAKEN.getKey());
        }

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setPhoneNumber(phoneNumber);
        return userRepo.save(user);
    }

    @Override
    @Transactional
    public ResponseMessage createUser(ReqUserCreate request) {
        User currentUser = this.getCurrentUser();

        UUID resolvedSchoolId = userScopeService.resolveSchoolUuid(request.getSchoolId());

        User savedUser = createUserAccount(request.getFirstName(),
                request.getLastName(),
                request.getUsername(),
                request.getPassword(),
                request.getRole(),
                request.getPhoneNumber()
        );
        if (CoreUtils.isPresent(request.getAddress())) {
            savedUser.setAddress(request.getAddress());
        }

        School school = (resolvedSchoolId != null) ? schoolRepo.findByUuid(resolvedSchoolId).orElse(null) : null;

        switch (request.getRole()) {
            case SCHOOL_DIRECTOR -> {
                SchoolDirector sd = new SchoolDirector();
                sd.setUser(savedUser);
                UUID orgUuid = (request.getOrganizationUuid() != null) ? request.getOrganizationUuid() :
                        (currentUser.getRole().equals(UserRole.SCHOOL_DIRECTOR) ?
                                schoolDirectorRepo.findByUser(currentUser).get().getOrganization().getUuid() : null);
                if (orgUuid != null) sd.setOrganization(organizationRepository.findByUuid(orgUuid).get());
                schoolDirectorRepo.save(sd);
            }
            case SCHOOL_ADMIN -> {
                SchoolAdmin sa = new SchoolAdmin();
                sa.setUser(savedUser);
                sa.setSchool(school);
                schoolAdminRepo.save(sa);
            }
            case STUDENT -> {
                Student s = new Student();
                s.setUser(savedUser);
                s.setSchool(school);
                Student save = studentRepo.save(s);
                if (request.getGroupUuids() != null && !request.getGroupUuids().isEmpty()) {
                    // Multi-group: enroll the new student into every requested group (additive).
                    for (UUID groupUuid : request.getGroupUuids()) {
                        Group targetGroup = groupRepository.findByUuid(groupUuid)
                                .orElseThrow(() -> new EntityNotFoundException(MessageKey.GROUP_NOT_FOUND.getKey()));

                        authToViewEntity.authorizeActionUponSchoolBroadAccess(targetGroup.getBranch().getSchool());

                        // The first group sets the student's home school when none was provided.
                        if (save.getSchool() == null) {
                            save.setSchool(targetGroup.getBranch().getSchool());
                            save = studentRepo.save(save);
                        }

                        studentEnrollmentHelper.enrollToGroup(save, targetGroup);
                    }
                    return new ResponseMessage("Student created and enrolled successfully.");
                }
            }
            case TEACHER -> {
                Teacher t = new Teacher();
                t.setUser(savedUser);
                t.setSchool(school);
                schoolAcademicConfigRepo.findBySchool_Uuid(school.getUuid())
                        .ifPresentOrElse(c -> t.setMonthlyCoinAllowance(c.getTeacherMonthlyCoinLimit()), () -> t.setMonthlyCoinAllowance(5000L));
                teacherRepository.save(t);
            }
        }
        return new ResponseMessage("User created successfully.");
    }


    @Override
    @Transactional
    public ResponseMessage deleteUser(UUID userId, String note) {
        User targetUser = userRepo.findByUuid(userId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        if (!deactivateUser(targetUser, this.getCurrentUser(), note)) {
            return new ResponseMessage("User is already deactivated.");
        }

        return new ResponseMessage("User deactivated successfully.");
    }

    @Override
    @Transactional
    public ResponseMessage deleteUsers(ReqUserDeleteList request) {
        if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new ValidationException(MessageKey.LIST_EMPTY.getKey());
        }

        List<UUID> userIds = request.getUserIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<User> targetUsers = userRepo.findAllByUuidIn(userIds);
        if (targetUsers.size() != userIds.size()) {
            throw new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey());
        }

        User currentUser = this.getCurrentUser();
        int deactivated = 0;
        for (User targetUser : targetUsers) {
            if (deactivateUser(targetUser, currentUser, request.getNote())) {
                deactivated++;
            }
        }

        int skipped = targetUsers.size() - deactivated;
        return new ResponseMessage(deactivated + " user(s) deactivated successfully."
                + (skipped > 0 ? " " + skipped + " user(s) were already deactivated." : ""));
    }


    private boolean deactivateUser(User targetUser, User currentUser, String note) {
        UUID userId = targetUser.getUuid();

        if (targetUser.getStatus().equals(UserStatus.BLOCKED)) {
            return false;
        }

        if (!currentUser.getRole().equals(UserRole.SYS_ADMIN)) {

            if (targetUser.getRole().equals(UserRole.SCHOOL_DIRECTOR)) {
                throw new PermissionForbidden(MessageKey.DIRECTOR_RESTRICTION.getKey());
            }

            School targetSchool = switch (targetUser.getRole()) {
                case STUDENT -> studentRepo.findByUser(targetUser).map(Student::getSchool).orElse(null);
                case TEACHER -> teacherRepository.findByUser(targetUser).map(Teacher::getSchool).orElse(null);
                case SCHOOL_ADMIN -> schoolAdminRepo.findByUser(targetUser).map(SchoolAdmin::getSchool).orElse(null);
                default -> null;
            };

            if (targetSchool == null) {
                throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
            }
            authToViewEntity.authorizeActionUponSchoolBroadAccess(targetSchool);
        }
        List<AttendanceRecord> allByStudentUserUuid = attendanceRecordRepository.findAllByStudent_User_Uuid(userId);
        attendanceRecordRepository.deleteAll(allByStudentUserUuid);
        targetUser.setStatus(UserStatus.BLOCKED);
        targetUser.setStatusNote(note);

        targetUser.setUsername(targetUser.getUsername() + "::deleted::" + UUID.randomUUID());
        userRepo.save(targetUser);

        if (targetUser.getRole().equals(UserRole.STUDENT)) {
            // Multi-group: close ALL ongoing enrollments, not just one.
            List<Enrollment> ongoing = enrollmentRepository.findAllByStudent_User_UuidAndStatus(userId, EnrollmentStatus.ONGOING);
            ongoing.forEach(e -> {
                e.setStatus(EnrollmentStatus.DELETED);
                e.setNote(note);
            });
            enrollmentRepository.saveAll(ongoing);
        }

        return true;
    }

    @Override
    @Transactional
    public ResponseMessage freezeUsers(ReqUserFreezeList request) {
        if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new ValidationException(MessageKey.LIST_EMPTY.getKey());
        }

        List<UUID> userIds = request.getUserIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<User> targetUsers = userRepo.findAllByUuidIn(userIds);
        if (targetUsers.size() != userIds.size()) {
            throw new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey());
        }

        User currentUser = this.getCurrentUser();
        int frozen = 0;
        for (User targetUser : targetUsers) {
            if (freezeUser(targetUser, currentUser, request.getNote())) {
                frozen++;
            }
        }

        int skipped = targetUsers.size() - frozen;
        return new ResponseMessage(frozen + " user(s) frozen successfully."
                + (skipped > 0 ? " " + skipped + " user(s) were already frozen." : ""));
    }

  
    private boolean freezeUser(User targetUser, User currentUser, String note) {
        if (targetUser.getStatus().equals(UserStatus.FROZEN)) {
            return false;
        }

        if (!currentUser.getRole().equals(UserRole.SYS_ADMIN)) {

            if (targetUser.getRole().equals(UserRole.SCHOOL_DIRECTOR)) {
                throw new PermissionForbidden(MessageKey.DIRECTOR_RESTRICTION.getKey());
            }

            School targetSchool = switch (targetUser.getRole()) {
                case STUDENT -> studentRepo.findByUser(targetUser).map(Student::getSchool).orElse(null);
                case TEACHER -> teacherRepository.findByUser(targetUser).map(Teacher::getSchool).orElse(null);
                case SCHOOL_ADMIN -> schoolAdminRepo.findByUser(targetUser).map(SchoolAdmin::getSchool).orElse(null);
                default -> null;
            };

            if (targetSchool == null) {
                throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
            }
            authToViewEntity.authorizeActionUponSchoolBroadAccess(targetSchool);
        }

        targetUser.setStatus(UserStatus.FROZEN);
        targetUser.setStatusNote(note);
        userRepo.save(targetUser);

        return true;
    }

    @Override
    @Transactional
    public ResponseMessage undeleteUser(UUID userId) {
        User userToUndelete = userRepo.findByUuid(userId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        if (!userToUndelete.getStatus().equals(UserStatus.BLOCKED)) {
            return new ResponseMessage("User is already active.");
        }

        User currentUser = this.getCurrentUser();
        if (currentUser.getRole().equals(UserRole.SCHOOL_ADMIN)) {
            UUID currentUserSchoolUuid = userScopeService.getCurrentUserSchoolUuid();
            ResSchoolInfo targetUserSchoolInfo = userScopeService.getSchoolInfoForUser(userToUndelete);
            if (targetUserSchoolInfo == null || !currentUserSchoolUuid.equals(targetUserSchoolInfo.getUuid())) {
                throw new PermissionForbidden(MessageKey.SCHOOL_CONTEXT_REQUIRED.getKey());
            }
        } else if (!currentUser.getRole().equals(UserRole.SYS_ADMIN)) {
            throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
        }

        String originalUsername = userToUndelete.getUsername().split("::deleted::")[0];

        if (userRepo.existsByUsername(originalUsername)) {
            Logger.logWarn("UNDELETE CONFLICT: Username '" + originalUsername + "' is taken. User " + userId + " remains BLOCKED.");
            return new ResponseMessage("SKIPPED_DUE_TO_CONFLICT");
        }

        userToUndelete.setUsername(originalUsername);
        userToUndelete.setStatus(UserStatus.ACTIVE);
        userRepo.save(userToUndelete);


        if (userToUndelete.getRole().equals(UserRole.STUDENT)) {
            // Multi-group: restore every deleted enrollment whose group is still active.
            List<Enrollment> toRestore = enrollmentRepository
                    .findAllByStudent_User_UuidAndStatus(userId, EnrollmentStatus.DELETED).stream()
                    .filter(e -> e.getGroup().getGroupStatus().equals(GroupStatus.ACTIVE))
                    .collect(Collectors.toList());
            if (!toRestore.isEmpty()) {
                toRestore.forEach(e -> e.setStatus(EnrollmentStatus.ONGOING));
                enrollmentRepository.saveAll(toRestore);
            } else {
                Logger.logInfo("No deleted enrollments in active groups to restore.");
            }
        }

        return new ResponseMessage("User restored successfully.");
    }

    @Override
    public void validateStudentLimit(UUID schoolUuid, int newStudentsCount) {
        School school = schoolRepo.findByUuid(schoolUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));

        if (school.getSubscriptionPlan() == null) {
            if (school.getStudentCount() + newStudentsCount > 1000) {
                throw new ValidationException(MessageKey.STUDENT_LIMIT_REACHED.getKey());
            }
            return;
        }

        int maxAllowed = school.getSubscriptionPlan().getMaxStudents();
        if (maxAllowed < 0) return;

        if (school.getOrganization() != null) {
            List<UUID> orgSchoolUuids = school.getOrganization().getSchools()
                    .stream().map(School::getUuid).toList();

            long totalStudentsInOrg = studentRepo.countActiveStudentsMulti(orgSchoolUuids);

            if (totalStudentsInOrg + newStudentsCount > maxAllowed) {
                throw new ValidationException(MessageKey.STUDENT_LIMIT_REACHED.getKey());
            }
        } else {
            if (school.getStudentCount() + newStudentsCount > maxAllowed) {
                throw new ValidationException(MessageKey.STUDENT_LIMIT_REACHED.getKey());
            }
        }
    }

    @Override
    @Transactional
    public ResponseMessage registerStudentViaReferral(ReqReferralRegister request) {
        // 1. Find the group from the code
        Group referredGroup = groupRepository.findByReferralCode(request.getReferralCode())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.REFERRAL_INVALID.getKey()));

        School targetSchool = referredGroup.getBranch().getSchool();
        UUID schoolUuid = targetSchool.getUuid();

        // 2. Validate branch capacity (Respects the branch's specific plan limit)
        this.validateStudentLimit(schoolUuid, 1);

        if (referredGroup.getGroupStatus() != GroupStatus.ACTIVE) {
            throw new ValidationException(MessageKey.REFERRAL_INACTIVE.getKey());
        }

        // 3. Check username uniqueness within the specific target school
        if (userRepo.existsByUsernameAndSchoolUuid(request.getUsername(), schoolUuid)) {
            throw new ValidationException(MessageKey.USERNAME_TAKEN.getKey());
        }

        if (request.getPassword().length() < 5 || request.getUsername().length() < 5) {
            throw new ValidationException(MessageKey.USERNAME_TOO_SHORT.getKey());
        }

        // 4. Create the User in REFERRED status
        User newUser = new User();
        newUser.setFirstName(request.getFirstName());
        newUser.setLastName(request.getLastName());
        newUser.setUsername(request.getUsername());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setRole(UserRole.STUDENT);
        newUser.setStatus(UserStatus.REFERRED);
        userRepo.save(newUser);

        // 5. Create Student profile linked to the branch
        Student student = new Student();
        student.setUser(newUser);
        student.setSchool(targetSchool);
        studentRepo.save(student);

        // 6. Create the Enrollment
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setGroup(referredGroup);
        enrollment.setStatus(EnrollmentStatus.ONGOING);
        enrollmentRepository.save(enrollment);

        return new ResponseMessage("Registration successful! Your school administrator will review your application.");
    }

    @Override
    @Transactional
    public ResponseMessage hardDeleteUser(UUID userId) {
        User user = userRepo.findByUuid(userId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

//        switch (user.getRole()) {
//            case MENTOR -> mentorRepo.findByUser(user).ifPresent(mentor -> {
//                schoolMentorRepo.deleteAll(schoolMentorRepo.findAllByMentorUserUuid(user.getUuid()));
//                mentorRepo.delete(mentor);
//            });
//            case MODERATOR -> moderatorRepo.findByUser(user).ifPresent(moderatorRepo::delete);
//            case SCHOOL_ADMIN -> schoolAdminRepo.findByUser(user).ifPresent(schoolAdminRepo::delete);
//            case STUDENT -> studentRepo.findByUser(user).ifPresent(studentRepo::delete);
//                case SYS_ADMIN -> throw new PermissionForbidden("System Administrator cannot be deleted.");
//        }
//
//        userRepo.delete(user);

        return new ResponseMessage("can't hard delete");
    }


    public void authorizeActionUponTeacher(Teacher teacher) {
        User currentUser = this.getCurrentUser();

        if (currentUser.getRole().equals(UserRole.SYS_ADMIN)) {
            return;
        }
        UUID studentSchoolUuid = teacher.getSchool().getUuid();
        UUID userSchoolUuid = userScopeService.getCurrentUserSchoolUuid();
        if (!userSchoolUuid.equals(studentSchoolUuid)) {
            throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
        }
    }


    public void authorizeActionUponSchool(School school) {
        User user = this.getCurrentUser();
        if (user.getRole().equals(UserRole.SYS_ADMIN)) {
            return;
        }
        if (user.getRole().equals(UserRole.SCHOOL_ADMIN)) {
            UUID uuid = userScopeService.getCurrentUserSchoolUuid();
            if (!school.getUuid().equals(uuid)) {
                throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
            }
        } else {
            throw new PermissionForbidden(MessageKey.SCHOOL_CONTEXT_REQUIRED.getKey());
        }
    }


    public void authorizeActionUponStudent(Student student) {
        User currentUser = this.getCurrentUser();

        if (currentUser.getRole().equals(UserRole.SYS_ADMIN)) {
            return;
        }

        UUID studentSchoolUuid = student.getSchool().getUuid();
        UUID userSchoolUuid = userScopeService.getCurrentUserSchoolUuid();

        if (!userSchoolUuid.equals(studentSchoolUuid)) {
            throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
        }

        if (currentUser.getRole().equals(UserRole.TEACHER)) {
            // Multi-group: a teacher may act on the student if they teach at least one of the
            // student's ongoing groups. If the student has ongoing groups but none are taught by
            // this teacher, access is denied (preserves the original single-group semantics).
            List<Enrollment> ongoing = enrollmentRepository
                    .findAllByStudent_User_UuidAndStatus(student.getUser().getUuid(), EnrollmentStatus.ONGOING);
            boolean teaches = ongoing.stream()
                    .anyMatch(e -> e.getGroup().getTeacher() != null
                            && e.getGroup().getTeacher().getUser().getUuid().equals(currentUser.getUuid()));
            if (!ongoing.isEmpty() && !teaches) {
                throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
            }
        }
    }

}
