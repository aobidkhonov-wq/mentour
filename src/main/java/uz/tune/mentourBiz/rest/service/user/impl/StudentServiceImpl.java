package uz.tune.mentourBiz.rest.service.user.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.exception.UnauthorizedAccessException;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.SchoolAcademicConfig;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.CourseLesson;
import uz.tune.mentourBiz.rest.domain.schoolManagement.schedule.AttendanceRecord;
import uz.tune.mentourBiz.rest.domain.schoolManagement.schedule.GroupSchedule;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.*;
import uz.tune.mentourBiz.rest.payload.req.student.*;
import uz.tune.mentourBiz.rest.payload.req.user.ReqApproveUserList;
import uz.tune.mentourBiz.rest.payload.req.user.ReqDeclineUserList;
import uz.tune.mentourBiz.rest.payload.res.ResAtRiskStudent;
import uz.tune.mentourBiz.rest.payload.res.ResFinanceHistoryWrapper;
import uz.tune.mentourBiz.rest.payload.res.ResFinanceTransaction;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;
import uz.tune.mentourBiz.rest.payload.res.student.ResBulkStudentResult;
import uz.tune.mentourBiz.rest.payload.res.studentApp.ResStudentHomeProfile;
import uz.tune.mentourBiz.rest.payload.res.user.ResStudentForLesson;
import uz.tune.mentourBiz.rest.payload.res.user.ResStudentList;
import uz.tune.mentourBiz.rest.payload.res.user.ResStudentOne;
import uz.tune.mentourBiz.rest.repository.FinanceTransactionRepo;
import uz.tune.mentourBiz.rest.repository.SchoolAcademicConfigRepo;
import uz.tune.mentourBiz.rest.repository.course.CourseLessonRepo;
import uz.tune.mentourBiz.rest.repository.group.GroupRepository;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.schedule.AttendanceRecordRepository;
import uz.tune.mentourBiz.rest.repository.schedule.GroupScheduleRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.transaction.TransactionRepo;
import uz.tune.mentourBiz.rest.repository.unit.VocabularyAnswerRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseAnswersRepository;
import uz.tune.mentourBiz.rest.repository.user.SchoolAdminRepo;
import uz.tune.mentourBiz.rest.repository.user.TeacherRepository;
import uz.tune.mentourBiz.rest.repository.user.UserRepo;
import uz.tune.mentourBiz.rest.service.SubscriptionValidator;
import uz.tune.mentourBiz.rest.service.exercise.impl.ProgressService;
import uz.tune.mentourBiz.rest.service.group.enrollment.StudentEnrollmentHelper;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.rest.service.school.SchoolService;
import uz.tune.mentourBiz.rest.service.user.StudentService;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;
import uz.tune.mentourBiz.rest.service.util.MessageSingleton;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepo studentRepo;
    private final MessageSingleton messageSingleton;
    private final UserService userService;
    private final UserScopeService userScopeService;
    private final SchoolRepo schoolRepo;
    private final GroupRepository groupRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepo userRepo;
    private final CourseLessonRepo courseLessonRepo;
    private final StudentEnrollmentHelper studentEnrollmentHelper;
    private final AuthToViewEntity authToViewEntity;
    private final TeacherRepository teacherRepository;
    private final SchoolService schoolService;
    private final SubscriptionValidator subscriptionValidator;
    private final TransactionRepo transactionRepo;
    private final FinanceTransactionRepo financeTransactionRepo;
    private final SchoolAcademicConfigRepo schoolAcademicConfigRepo;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final GroupScheduleRepository groupScheduleRepository;
    private final ProgressService progressService;
    private final ExerciseAnswersRepository exerciseAnswersRepository;
    private final VocabularyAnswerRepository vocabularyAnswerRepository;
    private final SchoolAdminRepo schoolAdminRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public ResStudentOne getStudentByUuid(UUID uuid) {
        Student student = studentRepo.getStudentByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        authToViewEntity.authorizeActionUponStudent(student);

        // At-risk — yagona canonical hisob (UserService.calculateIsAtRisk) bilan:
        // per-group attendance + live overall score, config thresholdlari bo'yicha.
        SchoolAcademicConfig config = schoolAcademicConfigRepo.findBySchool_Uuid(student.getSchool().getUuid())
                .orElse(new SchoolAcademicConfig());
        boolean atRisk = userService.calculateIsAtRisk(student, config);
        // Null-safe: existing rows may have a NULL is_atrisk after the column was added.
        if (!Boolean.valueOf(atRisk).equals(student.getIsAtRisk())) {
            student.setIsAtRisk(atRisk);
            studentRepo.save(student);
        }

        return new ResStudentOne(student);
    }

    @Override
    @Transactional(readOnly = true)
    public ResStudentHomeProfile getStudentHomeProfile() {
        User currentUser = userService.getCurrentUser();
        Student student = studentRepo.findByUser(currentUser)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));


        Enrollment activeEnrollment = enrollmentRepository
                .findTopByStudent_User_UuidAndStatus(currentUser.getUuid(), EnrollmentStatus.ONGOING)
                .orElseThrow(() -> new UnauthorizedAccessException("No active enrollment found for student."));


        UUID groupUuid = activeEnrollment.getGroup().getUuid();
        Long studentBalance = student.getLifeTimeCoinBalance();

        UUID schoolUuid = student.getSchool().getUuid();
        Integer betterStudentsCount = studentRepo.countStudentsWithHigherScoreInSchool(schoolUuid, student.getScore());

        Integer rank = betterStudentsCount + 1;

        return new ResStudentHomeProfile(student, rank,
                "GROUP RANKING", activeEnrollment.getGroup().getBranch().getSchool(), activeEnrollment.getGroup().getLevel());
    }

    @Override
    public Page<ResStudentList> getAllStudents(Integer page, Integer size, UserStatus status, String fullName,
                                               UUID schoolUuid, UUID classId, UUID courseId) {
        User user = userService.getCurrentUser();

        UUID resolvedId = userScopeService.resolveSchoolUuid(schoolUuid);

        Collection<UUID> schoolUuids;
        if (resolvedId != null) {
            schoolUuids = List.of(resolvedId);
        } else {
            schoolUuids = userScopeService.getAuthorizedSchoolUuids();
        }
        SchoolAcademicConfig schoolAcademicConfig;
        Optional<SchoolAcademicConfig> schoolAcademicConfigOptional = schoolAcademicConfigRepo.findBySchool_Uuid(resolvedId);
        if (schoolAcademicConfigOptional.isEmpty()) {
            schoolAcademicConfig = new SchoolAcademicConfig();
            schoolAcademicConfig.setAttendanceThreshold(0);
            schoolAcademicConfig.setMinScoreToPass(0);
        } else {
            schoolAcademicConfig = schoolAcademicConfigOptional.get();
        }
        UUID targetTeacherUuid = (user.getRole().equals(UserRole.TEACHER)) ? user.getUuid() : null;

        int l = studentRepo.countBySchool_UuidIn(schoolUuids);
        Sort sorted = Sort.by(Sort.Order.asc("user.lastName"));
        Pageable pageable = (page == null && size == null)
                ? PageRequest.of( 0, l, sorted)
                : PageRequest.of(page == null ? 0 : page, size == null ? l : size, sorted);
        if(!CoreUtils.isEmpty(status)&&UserStatus.UNASSIGNED.equals(status)){
            return studentRepo.findUnassignedMulti(schoolUuids,pageable).map(ResStudentList::new);
        }
        return studentRepo.findWithFilters(
                schoolUuids,
                targetTeacherUuid,
                status,
                fullName,
                classId,
                courseId,
                pageable
        ).map(student -> {
            student.setIsAtRisk(calculateIsAtRisk(student, schoolAcademicConfig));
            return new ResStudentList(student);
        });
    }

    private boolean calculateIsAtRisk(Student student, SchoolAcademicConfig config) {
        UUID userUuid = student.getUser().getUuid();

        List<AttendanceRecord> attendanceRecords = attendanceRecordRepository.findAllByStudent_User_Uuid(userUuid);
        long totalConducted = attendanceRecords.stream().filter(AttendanceRecord::getIsMarked).count();
        long attended = attendanceRecords.stream()
                .filter(r -> r.getIsMarked() && (r.getStatus() == AttendanceStatus.PRESENT || r.getStatus() == AttendanceStatus.LATE))
                .count();
        int attendanceRate = (totalConducted > 0) ? (int) ((attended * 100) / totalConducted) : 100;

        Integer minScoreToPass = config.getMinScoreToPass();
        Integer attendanceThreshold = config.getAttendanceThreshold();

        boolean scoreAtRisk = minScoreToPass != null && student.getScore() != null && student.getScore() < minScoreToPass;
        boolean attendanceAtRisk = attendanceThreshold != null && attendanceRate < attendanceThreshold;

        return scoreAtRisk || attendanceAtRisk;
    }

    @Override
    public ResFinanceHistoryWrapper getStudentsBalanceHistory(UUID userUuid, Pageable pageable, FinanceEnums.PaymentMethod method, Instant from, Instant to) {
        User user = userService.getCurrentUser();

        Student student = studentRepo.findByUserUuid(userUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        if (!user.getRole().equals(UserRole.SYS_ADMIN) && !user.getRole().equals(UserRole.STUDENT)) {
            authToViewEntity.authorizeActionUponStudent(student);
        } else if (user.getRole().equals(UserRole.STUDENT)) {
            if (!user.getUuid().equals(userUuid)) throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
        }

        Instant effectiveTo = (to != null) ? to : Instant.now();

        Page<ResFinanceTransaction> page = financeTransactionRepo.findAllHistoryByStudentUserUuid(userUuid, method, from, effectiveTo, pageable)
                .map(ResFinanceTransaction::new);

        return new ResFinanceHistoryWrapper(page, null);
    }

    @Override
    public ResFinanceHistoryWrapper getStudentsHistoryByType(UUID userUuid, FinanceEnums.FinanceTransactionType type, FinanceEnums.PaymentMethod method, Pageable pageable, Instant from, Instant to) {
        User user = userService.getCurrentUser();

        Student student = studentRepo.findByUserUuid(userUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        if (!user.getRole().equals(UserRole.SYS_ADMIN) && !user.getRole().equals(UserRole.STUDENT)) {
            authToViewEntity.authorizeActionUponStudent(student);
        } else if (user.getRole().equals(UserRole.STUDENT)) {
            if (!user.getUuid().equals(userUuid)) throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
        }

        Instant effectiveTo = (to != null) ? to : Instant.now();

        Page<ResFinanceTransaction> page = financeTransactionRepo.findHistoryByTypeByStudentUserUuid(userUuid, type, method, from, effectiveTo, pageable)
                .map(ResFinanceTransaction::new);

        return new ResFinanceHistoryWrapper(page, null);
    }

    @Override
    public List<ResStudentList> getAllStudents(UserStatus status) {
        User user = userService.getCurrentUser();
        List<Student> students;

        switch (user.getRole()) {
            case SYS_ADMIN -> students = (status == null)
                    ? studentRepo.findAll()
                    : studentRepo.findAllByUserStatus(status);
            case SCHOOL_ADMIN, MODERATOR, TEACHER -> {
                UUID userSchoolId = userScopeService.getCurrentUserSchoolUuid();
                UserStatus filterStatus = (status == null) ? UserStatus.ACTIVE : status;
                students = studentRepo.findAllByUserStatusAndSchool_Uuid(filterStatus, userSchoolId);
            }
            default -> throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
        }
        return students.stream().map(ResStudentList::new).collect(Collectors.toList());
    }


    //FIXME need security
    @Override
    @Transactional
    public ResponseMessage createStudents(ReqStudentCreate reqCreateStudents) {
        User currentUser = userService.getCurrentUser();
        UUID targetSchoolUuid = null;
        if (UserRole.SCHOOL_ADMIN.equals(currentUser.getRole())) {
            schoolAdminRepo.findByUser(currentUser).ifPresent(schoolAdmin -> {
                reqCreateStudents.setSchoolUuid(schoolAdmin.getSchool().getUuid());
            });
        } else if (UserRole.TEACHER.equals(currentUser.getRole())) {
            teacherRepository.findByUser(currentUser).ifPresent(teacher -> {
                reqCreateStudents.setSchoolUuid(teacher.getSchool().getUuid());
            });
        }
        targetSchoolUuid = userScopeService.resolveSchoolUuid(reqCreateStudents.getSchoolUuid());

        if (targetSchoolUuid == null) {
            throw new ValidationException(MessageKey.SCHOOL_CONTEXT_REQUIRED.getKey());
        }

        School school = schoolRepo.findByUuid(targetSchoolUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));
        subscriptionValidator.validateStudentAddition(school, 1);


            User newUser = userService.createUserAccount(
                    reqCreateStudents.getFirstName(),
                    reqCreateStudents.getLastName(),
                    reqCreateStudents.getUsername(),
                    reqCreateStudents.getPassword(), UserRole.STUDENT,reqCreateStudents.getPhoneNumber());

            newUser.setGender(reqCreateStudents.getGender());
            newUser.setEmail(reqCreateStudents.getEmail());
            newUser.setDateOfBirth(reqCreateStudents.getDateOfBirth());
            userRepo.save(newUser);

            Student student = new Student();
            student.setUser(newUser);
            student.setCurrentBalance(reqCreateStudents.getCurrentBalance() != null ? reqCreateStudents.getCurrentBalance() : 0L);
            student.setSchool(school);
            studentRepo.save(student);

            if (reqCreateStudents.getGroupUuids() != null) {
                List<Group> groups = groupRepository.findAllByUuidIn(reqCreateStudents.getGroupUuids());
                for (Group group : groups) {
                    Enrollment enrollment = new Enrollment();
                    enrollment.setStudent(student);
                    enrollment.setGroup(group);
                    enrollment.setStatus(EnrollmentStatus.ONGOING);
                    enrollmentRepository.save(enrollment);
                }
            }
        return new ResponseMessage("Students created successfully.");
    }

    @Override
    @Transactional
    public ResBulkStudentResult createStudentsForGroup(UUID schoolId, List<ReqBulkStudent> requests) {
        School school = schoolRepo.findByUuid(schoolId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));
        authToViewEntity.authorizeActionUponSchoolBroadAccess(school);

        if (requests == null || requests.isEmpty()) {
            return new ResBulkStudentResult(0, new ArrayList<>());
        }
        subscriptionValidator.validateStudentAddition(school, requests.size());

        int created = 0;
        List<ResBulkStudentResult.Failed> failed = new ArrayList<>();

        for (ReqBulkStudent req : requests) {
            String username = (req.getUsername() != null) ? req.getUsername().trim() : "";

            // 1. Required fields
            if (isBlank(req.getFirstName()) || isBlank(req.getLastName()) || username.isBlank() || req.getGroupId() == null) {
                failed.add(new ResBulkStudentResult.Failed(username, messageSingleton.getMessage(MessageKey.INVALID_ARGUMENT)));
                continue;
            }

            // 2. Username must be unique
            if (userRepo.existsByUsername(username)) {
                failed.add(new ResBulkStudentResult.Failed(username,
                        messageSingleton.getMessage(MessageKey.USERNAME_TAKEN.getKey(), username)));
                continue;
            }

            // 3. Group must exist and belong to the path school
            Group group = groupRepository.findByUuid(req.getGroupId()).orElse(null);
            if (group == null) {
                failed.add(new ResBulkStudentResult.Failed(username,
                        messageSingleton.getMessage(MessageKey.GROUP_NOT_FOUND.getKey(), String.valueOf(req.getGroupId()))));
                continue;
            }
            if (!group.getBranch().getSchool().getUuid().equals(school.getUuid())) {
                failed.add(new ResBulkStudentResult.Failed(username,
                        messageSingleton.getMessage(MessageKey.GROUP_NO_ENROLLMENT.getKey(), username)));
                continue;
            }

            // 4. Create account (default password = phone number, fallback "1111"), student and enrollment
            String password = "12345";
            User user = userService.createUserAccount(
                    req.getFirstName().trim(), req.getLastName().trim(), username, password, UserRole.STUDENT,null);
            user.setPhoneNumber(req.getPhoneNumber());
            userRepo.save(user);

            Student student = new Student();
            student.setUser(user);
            student.setSchool(school);
            Student savedStudent = studentRepo.save(student);

            Enrollment enrollment = new Enrollment();
            enrollment.setStudent(savedStudent);
            enrollment.setGroup(group);
            enrollment.setStatus(EnrollmentStatus.ONGOING);
            enrollmentRepository.save(enrollment);

            created++;
        }

        return new ResBulkStudentResult(created, failed);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ResAtRiskStudent> getAtRiskStudents(UUID schoolUuid, Pageable pageable) {
        // 1. Resolve target school
        UUID targetSchoolUuid = userScopeService.resolveSchoolUuid(schoolUuid);
        if (targetSchoolUuid == null) throw new ValidationException(MessageKey.SCHOOL_CONTEXT_REQUIRED.getKey());

        // 2. Load thresholds
        SchoolAcademicConfig config = schoolAcademicConfigRepo.findBySchool_Uuid(targetSchoolUuid)
                .orElse(new SchoolAcademicConfig());

        // 3. Get all active students in the branch
        Page<Student> studentsPage = studentRepo.findAllBySchool_Uuid(targetSchoolUuid, pageable);
        List<ResAtRiskStudent> atRiskList = new ArrayList<>();

        for (Student student : studentsPage.getContent()) {
            if (student.getUser().getStatus() != UserStatus.ACTIVE) continue;

            // Get current active enrollment
            Optional<Enrollment> enrollmentOpt = enrollmentRepository.findTopByStudent_User_UuidAndStatus(student.getUser().getUuid(), EnrollmentStatus.ONGOING);
            if (enrollmentOpt.isEmpty()) continue;

            Group group = enrollmentOpt.get().getGroup();

            // --- Calculation 1: Attendance Rate (per-group, getOverallAttendance bilan bir xil) ---
            List<AttendanceRecord> records = attendanceRecordRepository
                    .findActiveForStudentInGroup(group.getUuid(), student.getUuid(), Instant.now());
            long total = records.size();
            long attended = records.stream()
                    .filter(r -> r.getStatus() == AttendanceStatus.PRESENT || r.getStatus() == AttendanceStatus.LATE)
                    .count();
            int attendanceRate = (total > 0) ? (int) Math.round(attended * 100.0 / total) : 100;

            // --- Calculation 2: Academic Average (jonli per-unit foizlar o'rtachasi) ---
            List<GroupSchedule> groupSchedules = groupScheduleRepository.findAllByGroup_UuidAndStatusOrderByDueDateAsc(group.getUuid(), GroupScheduleStatus.ACTIVE);
            List<Unit> units = groupSchedules.stream()
                    .map(GroupSchedule::getUnit)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            UUID stSchoolUuid = student.getSchool().getUuid();
            List<Integer> unitPercents = new ArrayList<>();
            for (Unit u : units) {
                long possible = progressService.calculateTotalPossible(u, stSchoolUuid);
                long earned = progressService.calculateTotalEarned(student, u.getUuid());
                unitPercents.add(progressService.toPercentage(earned, possible));
            }
            int avgResult = unitPercents.isEmpty() ? 0
                    : (int) Math.round(unitPercents.stream().mapToInt(Integer::intValue).average().orElse(0));


            if (attendanceRate < config.getAttendanceThreshold() || avgResult < config.getResultsThreshold()) {
                atRiskList.add(new ResAtRiskStudent(
                        student.getUuid(),
                        student.getUser().getFirstName() + " " + student.getUser().getLastName(),
                        student.getUser().getUsername(),
                        student.getUser().getAttachment() != null ? new ResAttachment(student.getUser().getAttachment()) : null,
                        group.getName(),
                        attendanceRate,
                        avgResult,
                        config.getAttendanceThreshold(),
                        config.getResultsThreshold()
                ));
            }
        }

        return new org.springframework.data.domain.PageImpl<>(atRiskList, pageable, studentsPage.getTotalElements());
    }


    @Override
    @Transactional
    public ResponseMessage updateStudent(UUID uuid, ReqStudentsUpdate req) {
        Student student = studentRepo.getStudentByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        authToViewEntity.authorizeActionUponStudent(student);

        User userToUpdate = student.getUser();
        if (CoreUtils.isPresent(req.getFirstName())) {
            userToUpdate.setFirstName(req.getFirstName());
        }
        if (CoreUtils.isPresent(req.getLastName())) {
            userToUpdate.setLastName(req.getLastName());
        }
        if (req.getStatus() != null) {
            userToUpdate.setStatus(req.getStatus());
        }
        if (CoreUtils.isPresent(req.getUsername()) && !userToUpdate.getUsername().equals(req.getUsername())) {
            if (userRepo.existsByUsername(req.getUsername())) {
                throw new ValidationException(MessageKey.USERNAME_TAKEN.getKey());
            }
            userToUpdate.setUsername(req.getUsername());
        }
        if (CoreUtils.isPresent(req.getPhoneNumber())) {
            userToUpdate.setPhoneNumber(req.getPhoneNumber());
        }
        if (CoreUtils.isPresent(req.getAddress())) {
            userToUpdate.setAddress(req.getAddress());
        }
        if (req.getGender() != null) {
            userToUpdate.setGender(req.getGender());
        }
        if (CoreUtils.isPresent(req.getEmail())) {
            userToUpdate.setEmail(req.getEmail());
        }
        if (req.getDateOfBirth() != null) {
            userToUpdate.setDateOfBirth(req.getDateOfBirth());
        }
        if (CoreUtils.isPresent(req.getPassword())) {
            userToUpdate.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        userRepo.save(userToUpdate);

        return new ResponseMessage("Student updated successfully.");
    }

    @Transactional
    public ResponseMessage deleteStudent(UUID userUuid, String note) {
        Student student = studentRepo.findByUser_Uuid(userUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        authToViewEntity.authorizeActionUponStudent(student);

        return userService.deleteUser(userUuid, note);
    }

    @Override
    @Transactional
    public ResponseMessage assignStudentsToClass(ReqStudentAssign req) {
        Group group = groupRepository.findByUuid(req.getGroupUuids())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.GROUP_NOT_FOUND.getKey()));
        authToViewEntity.authorizeActionUponSchoolBroadAccess(group.getBranch().getSchool());

        List<Student> students = studentRepo.findAllByUuidIn(req.getStudentUuids());
        for (Student student : students) {
            authToViewEntity.authorizeActionUponStudent(student);

            if (!enrollmentRepository.existsByStudentAndGroup(student, group)) {
                enrollmentRepository.findTopByStudent_User_UuidAndStatus(student.getUser().getUuid(), EnrollmentStatus.ONGOING)
                        .ifPresent(e -> {
                            e.setStatus(EnrollmentStatus.FINISHED);
                            enrollmentRepository.save(e);
                        });

                Enrollment enrollment = new Enrollment();
                enrollment.setStudent(student);
                enrollment.setGroup(group);
                enrollment.setStatus(EnrollmentStatus.ONGOING);
                enrollmentRepository.save(enrollment);
            }
        }
        return new ResponseMessage("Students assigned successfully.");
    }

    @Override
    @Transactional
    public ResponseMessage assignUnassignedStudentsToGroup(ReqUnassignedStudentAssign req) {
        Group group = groupRepository.findByUuid(req.getGroupUuid())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.GROUP_NOT_FOUND.getKey()));
        authToViewEntity.authorizeActionUponSchoolBroadAccess(group.getBranch().getSchool());

        List<Student> students = studentRepo.findAllByUserUuidIn(req.getUserUuids());
        int assignedCount = 0;
        for (Student student : students) {
            authToViewEntity.authorizeActionUponStudent(student);

//            boolean alreadyAssigned = enrollmentRepository
//                    .findTopByStudent_User_UuidAndStatus(student.getUser().getUuid(), EnrollmentStatus.ONGOING)
//                    .isPresent();
//            if (alreadyAssigned) {
//                continue;
//            }

            Enrollment enrollment = new Enrollment();
            enrollment.setStudent(student);
            enrollment.setGroup(group);
            enrollment.setStatus(EnrollmentStatus.ONGOING);
            enrollmentRepository.save(enrollment);
            assignedCount++;
        }
        return new ResponseMessage(assignedCount + " students assigned to group successfully.");
    }

    @Override
    @Transactional
    public ResponseMessage deAssignStudentFromClass(ReqStudentAssign req) {
        Group group = groupRepository.findByUuid(req.getGroupUuids())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.GROUP_NOT_FOUND.getKey()));

        authToViewEntity.authorizeActionUponSchoolBroadAccess(group.getBranch().getSchool());

        List<Student> students = studentRepo.findAllByUuidIn(req.getStudentUuids());
        for (Student student : students) {
            authToViewEntity.authorizeActionUponStudent(student);
            enrollmentRepository.findByStudentAndGroupAndStatus(student, group, EnrollmentStatus.ONGOING)
                    .ifPresent(e -> {
                        e.setStatus(EnrollmentStatus.FINISHED);
                        e.setNote(req.getNote());
                        enrollmentRepository.save(e);
                    });
            List<AttendanceRecord> allByStudentUserUuid = attendanceRecordRepository.findAllByStudentUuid(student.getUuid());
            attendanceRecordRepository.deleteAll(allByStudentUserUuid);
        }

        return new ResponseMessage("Students removed from class successfully.");
    }

    @Override
    @Transactional
    public List<ResStudentList> getStudentForClass(UUID groupId, String fullName) {
        Group group = groupRepository.findByUuid(groupId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.GROUP_NOT_FOUND.getKey()));

        List<Enrollment> enrollments = enrollmentRepository.findByGroupAndNameList(groupId, fullName).stream()
                .filter(e -> e.getStatus().equals(EnrollmentStatus.ONGOING))
                .collect(Collectors.toList());

        // Guruhning ACTIVE scheduled unitlari + jami mumkin bo'lgan ball (unit bo'yicha bir marta)
        List<GroupSchedule> groupSchedules = groupScheduleRepository
                .findAllByGroup_UuidAndStatusOrderByDueDateAsc(groupId, GroupScheduleStatus.ACTIVE);
        List<Unit> units = groupSchedules.stream()
                .map(GroupSchedule::getUnit)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<UUID> unitUuids = units.stream().map(Unit::getUuid).collect(Collectors.toList());

        UUID schoolUuid = enrollments.isEmpty() ? null : enrollments.get(0).getStudent().getSchool().getUuid();

        // At-risk thresholdlari — academic config (updateConfig) bilan bir xil canonical hisob.
        SchoolAcademicConfig atRiskConfig = (schoolUuid == null) ? new SchoolAcademicConfig()
                : schoolAcademicConfigRepo.findBySchool_Uuid(schoolUuid).orElse(new SchoolAcademicConfig());

        Map<UUID, Long> possibleByUnit = new HashMap<>();
        for (Unit u : units) {
            possibleByUnit.put(u.getUuid(), progressService.calculateTotalPossible(u, schoolUuid));
        }

        // Jami to'plangan ball — batch (exercise + vocab), studentId:unitUuid bo'yicha
        List<Long> studentIds = enrollments.stream().map(e -> e.getStudent().getId()).collect(Collectors.toList());
        Map<String, Long> earnedMap = new HashMap<>();
        if (!studentIds.isEmpty() && !unitUuids.isEmpty()) {
            for (Object[] row : exerciseAnswersRepository.sumEarnedGroupedByStudentsAndUnits(studentIds, unitUuids)) {
                earnedMap.merge(row[0] + ":" + row[1], ((Number) row[2]).longValue(), Long::sum);
            }
            for (Object[] row : vocabularyAnswerRepository.sumEarnedVocabGroupedByStudentsAndUnits(studentIds, unitUuids)) {
                earnedMap.merge(row[0] + ":" + row[1], ((Number) row[2]).longValue(), Long::sum);
            }
        }

        List<ResStudentList> result = new ArrayList<>();
        for (Enrollment e : enrollments) {
            Student student = e.getStudent();

            // At-risk statusni qayta hisoblab, o'zgargan bo'lsa bazaga yozamiz (academic config bilan bir xil).
            boolean atRisk = userService.calculateIsAtRisk(student, atRiskConfig);
            if (!Boolean.valueOf(atRisk).equals(student.getIsAtRisk())) {
                student.setIsAtRisk(atRisk);
                studentRepo.save(student);
            }

            ResStudentList dto = new ResStudentList(student);
            dto.setAtRisk(atRisk);

            // Overall attendance % — /api/v1/attendance (getOverallAttendance) bilan bir xil (per-group)
            List<AttendanceRecord> records = attendanceRecordRepository
                    .findActiveForStudentInGroup(group.getUuid(), student.getUuid(), Instant.now());
            long total = records.size();
            long attended = records.stream()
                    .filter(r -> r.getStatus() == AttendanceStatus.PRESENT || r.getStatus() == AttendanceStatus.LATE)
                    .count();
            dto.setOverallAttendancePercentage(total == 0 ? 0 : (int) Math.round(attended * 100.0 / total));

            // Overall score — jonli (live) per-unit foizlarning o'rtachasi (marks/course formulasi bilan bir xil)
            List<Integer> unitPercents = new ArrayList<>();
            for (UUID unitUuid : unitUuids) {
                long possible = possibleByUnit.getOrDefault(unitUuid, 0L);
                long earned = earnedMap.getOrDefault(student.getId() + ":" + unitUuid, 0L);
                unitPercents.add(progressService.toPercentage(earned, possible));
            }
            dto.setOverallScore(unitPercents.isEmpty() ? 0
                    : (int) Math.round(unitPercents.stream().mapToInt(Integer::intValue).average().orElse(0)));

            result.add(dto);
        }
        return result;
    }

    @Override
    @Transactional
    public ResponseMessage transferStudent(UUID studentUuid, UUID targetGroupUuid, boolean withBillingPlan) {
        Student student = studentRepo.findByUuid(studentUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
        Group targetGroup = groupRepository.findByUuid(targetGroupUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.GROUP_NOT_FOUND.getKey()));

        // Hierarchy Fix: Ensure both student and target group are in the Director's Org
        authToViewEntity.authorizeActionUponStudent(student);
        authToViewEntity.authorizeActionUponSchoolBroadAccess(targetGroup.getBranch().getSchool());

        studentEnrollmentHelper.transferToGroup(student, targetGroup, withBillingPlan);
        return new ResponseMessage("Student transferred successfully.");
    }

    @Override
    public List<ResStudentList> getSchoolStudentsToAddToClass(UUID groupUuid) {
        Group group = groupRepository.findByUuid(groupUuid).orElseThrow();

        // Security check
        authToViewEntity.authorizeActionUponSchoolBroadAccess(group.getBranch().getSchool());

        return studentRepo.findAvailableStudentsForGroup(group.getBranch().getSchool().getUuid(), group.getUuid())
                .stream().map(ResStudentList::new).toList();
    }


    @Override
    @Transactional
    public ResponseMessage deReferStudent(ReqStudentAssign req) {
        Group group = groupRepository.findByUuid(req.getGroupUuids())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.GROUP_NOT_FOUND.getKey()));

        authToViewEntity.authorizeActionUponSchoolBroadAccess(group.getBranch().getSchool());

        List<Student> students = studentRepo.findAllByUuidIn(req.getStudentUuids());
        for (Student student : students) {
            authToViewEntity.authorizeActionUponStudent(student);

            User u = student.getUser();
            u.setStatus(UserStatus.REFERRED);
            u.setStatusNote(req.getNote());
            userRepo.save(u);

            enrollmentRepository.findByStudentAndGroupAndStatus(student, group, EnrollmentStatus.ONGOING)
                    .ifPresent(e -> {
                        e.setStatus(EnrollmentStatus.FINISHED);
                        e.setNote(req.getNote());
                        enrollmentRepository.save(e);
                    });
        }
        return new ResponseMessage("Students moved back to referred status.");
    }

    //todo create similar for students for group

    @Override
    public List<ResStudentForLesson> getStudentsForLesson(UUID lessonId) {
        CourseLesson lesson = courseLessonRepo.findByUuid(lessonId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.COURSE_NOT_FOUND.getKey()));
        Group group = lesson.getCourse().getGroup();

        return enrollmentRepository.findAllByGroup_UuidAndStatus(lesson.getCourse().getGroup().getUuid(), EnrollmentStatus.ONGOING)
                .stream()
                .map(Enrollment::getStudent)
                .map(ResStudentForLesson::new)
                .collect(Collectors.toList());
    }
    //todo authorize

    @Override
    public List<ResStudentList> getReferredStudents(UUID groupId) {
        User user = userService.getCurrentUser();

        List<Student> referred;
        if (user.getRole().equals(UserRole.SYS_ADMIN)) {
            referred = studentRepo.findAllByUserStatus(UserStatus.REFERRED).stream()
                    .filter(s -> s.getEnrollments().stream()
                            .anyMatch(e -> e.getGroup().getUuid().equals(groupId)))
                    .collect(Collectors.toList());
        } else {
            Collection<UUID> authorizedUuids = userScopeService.getAuthorizedSchoolUuids();
            referred = studentRepo.findReferredByGroupAndOrg(groupId, authorizedUuids);
        }

        return referred.stream()
                .map(ResStudentList::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ResponseMessage approveStudents(ReqApproveUserList reqApproveUserList) {
        List<Student> studentsToApprove = studentRepo.findAllByUuidIn(reqApproveUserList.getStudentIds());
        if (studentsToApprove.isEmpty()) {
            throw new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey());
        }

        Map<UUID, List<Student>> studentsBySchool = studentsToApprove.stream()
                .collect(Collectors.groupingBy(s -> s.getSchool().getUuid()));

        for (Map.Entry<UUID, List<Student>> entry : studentsBySchool.entrySet()) {
            UUID schoolUuid = entry.getKey();
            int countToApprove = entry.getValue().size();

            userService.validateStudentLimit(schoolUuid, countToApprove);
        }

        for (Student s : studentsToApprove) {
            authToViewEntity.authorizeActionUponStudent(s);

            if (!s.getUser().getStatus().equals(UserStatus.REFERRED)) {
                throw new ValidationException(MessageKey.REFERRAL_ALREADY_ACTIVE.getKey());
            }
            s.getUser().setStatus(UserStatus.ACTIVE);
        }

        studentRepo.saveAll(studentsToApprove);
        return new ResponseMessage("Students approved and activated.");
    }

    @Override
    @Transactional
    public ResponseMessage declineStudents(ReqDeclineUserList req) {
        List<Student> studentsToDecline = studentRepo.findAllByUuidIn(req.getStudentIds());

        for (Student student : studentsToDecline) {
            authToViewEntity.authorizeActionUponStudent(student);
        }

        List<User> usersToDelete = studentsToDecline.stream().map(Student::getUser).toList();
        studentRepo.deleteAll(studentsToDecline);
        userRepo.deleteAll(usersToDelete);

        return new ResponseMessage(studentsToDecline.size() + " referral(s) declined.");
    }

    private void authorizeAction(Student student) {
        User currentUser = userService.getCurrentUser();
        if (currentUser.getRole() == UserRole.SYS_ADMIN) return;

        UUID userSchoolUuid = userScopeService.getCurrentUserSchoolUuid();
        if (!student.getSchool().getUuid().equals(userSchoolUuid)) {
            throw new PermissionForbidden(MessageKey.SCHOOL_CONTEXT_REQUIRED.getKey());
        }
        if (currentUser.getRole().equals(UserRole.TEACHER)) {
            // Multi-group: authorized only if the teacher teaches at least one ongoing group of the student.
            boolean teaches = enrollmentRepository
                    .findAllByStudent_User_UuidAndStatus(student.getUser().getUuid(), EnrollmentStatus.ONGOING)
                    .stream()
                    .anyMatch(e -> e.getGroup().getTeacher() != null
                            && e.getGroup().getTeacher().getUser().getUuid().equals(currentUser.getUuid()));
            if (!teaches) {
                throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
            }
        }
    }


    private void authorizeAction(School school) {
        User currentUser = userService.getCurrentUser();
        if (!currentUser.getRole().equals(UserRole.SYS_ADMIN)) {
            UUID userSchoolUuid = userScopeService.getCurrentUserSchoolUuid();
            if (!school.getUuid().equals(userSchoolUuid)) {
                throw new PermissionForbidden(MessageKey.SCHOOL_CONTEXT_REQUIRED.getKey());
            }
        }
        ;

    }
}