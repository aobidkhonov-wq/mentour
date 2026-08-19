package uz.tune.mentourBiz.rest.service.user.impl;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.shopManagement.coins.CoinTransaction;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.GroupStatus;
import uz.tune.mentourBiz.rest.enums.TransactionType;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.enums.UserStatus;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.shop.ResCoinTransaction;
import uz.tune.mentourBiz.rest.payload.res.user.ResTeacherOne;
import uz.tune.mentourBiz.rest.payload.studentReq.req.ReqAwardCoins;
import uz.tune.mentourBiz.rest.repository.group.GroupRepository;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.transaction.CoinTransactionRepository;
import uz.tune.mentourBiz.rest.repository.user.TeacherRepository;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.rest.service.user.TeacherService;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;
import uz.tune.mentourBiz.rest.service.util.MessageSingleton;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final UserService userService;
    private final TeacherRepository teacherRepository;
    private final StudentRepo studentRepository;
    private final CoinTransactionRepository coinTransactionRepository;
    private final MessageSingleton messageSingleton;
    private final UserScopeService userScopeService;
    private final StudentRepo studentRepo;
    private final AuthToViewEntity authToViewEntity;
    private final EnrollmentRepository enrollmentRepository;
    private final GroupRepository groupRepository;

    @Override
    @Transactional
    public ResponseMessage awardCoinsToStudent(List<ReqAwardCoins> request) {
        User currentUser = userService.getCurrentUser();

        // Logic Change: Directors/Admins don't have a "Monthly Allowance" limit
        boolean bypassLimit = currentUser.getRole().equals(UserRole.SCHOOL_DIRECTOR) || currentUser.getRole().equals(UserRole.SCHOOL_ADMIN);

        if (!bypassLimit) {
            int totalRequested = request.stream().mapToInt(ReqAwardCoins::getAmount).sum();
            Teacher teacher = teacherRepository.findByUser_Uuid(currentUser.getUuid()).orElseThrow();
            if (teacher.getMonthlyCoinAllowance() < totalRequested) throw new ValidationException(MessageKey.SHOP_TEACHER_LIMIT.getKey());
            teacher.setMonthlyCoinAllowance(teacher.getMonthlyCoinAllowance() - totalRequested);
            teacherRepository.save(teacher);
        }

        for (ReqAwardCoins reward : request) {
            Student student = studentRepo.findByUuid(reward.getStudentUuid()).orElseThrow();

            // Security: Check if student belongs to the Director's Organization
            authToViewEntity.authorizeActionUponStudent(student);

            studentRepo.addCoinsAtomic(student.getUuid(), reward.getAmount());

            CoinTransaction tx = new CoinTransaction();
            tx.setStudent(student);
            tx.setAmount(reward.getAmount());
            tx.setType(TransactionType.ADMINISTRATION_AWARD);
            tx.setGivenBy(currentUser.getUsername());
            coinTransactionRepository.save(tx);
        }
        return new ResponseMessage("Coins awarded successfully.");
    }

    @Override
    @Transactional(readOnly = true)
    public ResTeacherOne getOne(UUID uuid) {
        Teacher teacher = teacherRepository.findByUser_Uuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        authToViewEntity.authorizeActionUponTeacher(teacher);

        long totalStudents = enrollmentRepository.countTotalStudentsByTeacher(uuid);
        long activeClasses = groupRepository.countByTeacher_User_UuidAndGroupStatus(uuid, GroupStatus.ACTIVE);

        return new ResTeacherOne(teacher, totalStudents, activeClasses);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResTeacherOne> getAll(Pageable pageable, UserStatus status, UUID schoolId, String fullName) {
        User currentUser = userService.getCurrentUser();

        UUID resolvedId = userScopeService.resolveSchoolUuid(schoolId);

        Collection<UUID> schoolUuids;
        if (resolvedId != null) {
            schoolUuids = List.of(resolvedId);
        } else {
            schoolUuids = userScopeService.getAuthorizedSchoolUuids();
        }

        UserStatus filterStatus = (status == null) ? UserStatus.ACTIVE : status;

        Page<Teacher> teachers = teacherRepository.findWithFilters(schoolUuids, filterStatus, fullName, pageable);

        return teachers.map(teacher -> {
            UUID teacherUuid = teacher.getUser().getUuid();
            long totalStudents = enrollmentRepository.countTotalStudentsByTeacher(teacherUuid);
            long activeClasses = groupRepository.countByTeacher_User_UuidAndGroupStatus(teacherUuid, GroupStatus.ACTIVE);
            return new ResTeacherOne(teacher, totalStudents, activeClasses);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResCoinTransaction> getCoinHistory(Pageable pageable, TransactionType type, String studentName, String givenBy) {
        User currentUser = userService.getCurrentUser();

        Collection<UUID> schoolUuids = userScopeService.getAuthorizedSchoolUuids();

        UUID teacherUuid = currentUser.getRole().equals(UserRole.TEACHER) ? currentUser.getUuid() : null;

        return coinTransactionRepository.findAllFilteredMulti(
                schoolUuids,
                teacherUuid,
                type,
                studentName,
                givenBy,
                pageable
        ).map(ResCoinTransaction::new);
    }



}