package uz.tune.mentourBiz.rest.service.ranking.impl;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.Branch;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.EnrollmentStatus;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.payload.res.ranking.ResStudentForRanking;
import uz.tune.mentourBiz.rest.repository.course.CourseRepo;
import uz.tune.mentourBiz.rest.repository.group.GroupRepository;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.school.BranchRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.service.group.enrollment.StudentEnrollmentHelper;
import uz.tune.mentourBiz.rest.service.ranking.RankingService;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final UserService userService;
    private final UserScopeService userScopeService;
    private final StudentRepo studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepo courseRepo;
    private final GroupRepository groupRepository;
    private final BranchRepository branchRepository;
    private final SchoolRepo schoolRepo;
    private final StudentEnrollmentHelper studentEnrollmentHelper;

    //FIXME allow other roles to view these

    @Override
    public Page<ResStudentForRanking> getRankingByStudentGroup(Pageable pageable, UUID groupUuid) {
        User user = userService.getCurrentUser();
        Group group;
        Page<ResStudentForRanking> page;


        if(user.getRole().equals(UserRole.SYS_ADMIN)){
            page = enrollmentRepository
                    .findAllByGroup_UuidAndStatus(groupUuid, EnrollmentStatus.ONGOING, pageable)
                    .map(e -> new ResStudentForRanking(e.getStudent()));
        }
        else if(user.getRole().equals(UserRole.STUDENT)) {
            Group groupStudents = getStudentsGroup(groupUuid);
            page = enrollmentRepository
                    .findAllByGroup_UuidAndStatus(groupStudents.getUuid(), EnrollmentStatus.ONGOING, pageable)
                    .map(e -> new ResStudentForRanking(e.getStudent()));
        }
        else {
            UUID schoolUuid = userScopeService.getCurrentUserSchoolUuid();
            group = groupRepository.findByUuid(groupUuid)
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.GROUP_NOT_FOUND.getKey()));
            if(!group.getBranch().getSchool().getUuid().equals(schoolUuid)){
                throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
            }
            page = enrollmentRepository
                    .findAllByGroup_UuidAndStatus(groupUuid, EnrollmentStatus.ONGOING, pageable)
                    .map(e -> new ResStudentForRanking(e.getStudent()));
        }
        return page;
    }


    @Override
    public Page<ResStudentForRanking> getRankingByStudentBranch(Pageable pageable, UUID branchUuid) {
        User user = userService.getCurrentUser();
        Branch branch;
        Page<ResStudentForRanking> page;
        Pageable studentPageable = remapSortToStudentRoot(pageable);


        if(user.getRole().equals(UserRole.SYS_ADMIN)){
            page = studentRepository
                    .findDistinctRankingByBranch(branchUuid, studentPageable)
                    .map(ResStudentForRanking::new);
        }
        else if(user.getRole().equals(UserRole.STUDENT)) {
            Group group = getStudentsGroup(null);
            page = studentRepository
                    .findDistinctRankingByBranch(group.getBranch().getUuid(), studentPageable)
                    .map(ResStudentForRanking::new);
        }
        else {
            branch = branchRepository.findByUuid(branchUuid)
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.BRANCH_NOT_FOUND.getKey()));
            UUID schoolUuid = userScopeService.getCurrentUserSchoolUuid();
            if(!branch.getSchool().getUuid().equals(schoolUuid)){
              throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
            }
            page = studentRepository
                    .findDistinctRankingByBranch(branchUuid, studentPageable)
                    .map(ResStudentForRanking::new);
        }
        return page;
    }

    @Override
    public Page<ResStudentForRanking> getRankingByStudentSchool(Pageable pageable, UUID schoolUuid) {
        User user = userService.getCurrentUser();
//        schoolRepo.findByUuid(schoolUuid)
//                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));
        Page<ResStudentForRanking> page;
        Pageable studentPageable = remapSortToStudentRoot(pageable);


        if(user.getRole().equals(UserRole.SYS_ADMIN)){
            page = studentRepository
                    .findDistinctRankingBySchool(schoolUuid, studentPageable)
                    .map(ResStudentForRanking::new);
        }
        else if(user.getRole().equals(UserRole.STUDENT)) {
            Group group = getStudentsGroup(null);
            page = studentRepository
                    .findDistinctRankingBySchool(group.getBranch().getSchool().getUuid(), studentPageable)
                    .map(ResStudentForRanking::new);
        }
        else {
            UUID schoolUuidOfUser = userScopeService.getCurrentUserSchoolUuid();
            if(!schoolUuidOfUser.equals(schoolUuid)) {
                throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
            }
            page = studentRepository
                    .findDistinctRankingBySchool(schoolUuid, studentPageable)
                    .map(ResStudentForRanking::new);
        }
        return page;
    }

    /**
     * The ranking endpoints default their sort to {@code student.lifeTimeCoinBalance}, which is
     * correct when the query root is {@link Enrollment}. The branch/school ranking now queries
     * {@link Student} directly (to deduplicate students across groups), so any {@code student.*}
     * sort path has to be rewritten to be relative to the {@code Student} root.
     */
    private Pageable remapSortToStudentRoot(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }
        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            String property = order.getProperty();
            if (property.startsWith("student.")) {
                property = property.substring("student.".length());
            }
            orders.add(new Sort.Order(order.getDirection(), property, order.getNullHandling()));
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
    }

    private Group getStudentsGroup(UUID groupUuid) {
        User user = userService.getCurrentUser();
        Student student = studentRepository.findByUser_Uuid(user.getUuid())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        // Multi-group: optional groupUuid selects which ongoing group; null → primary group.
        return studentEnrollmentHelper.resolveActiveEnrollment(student, groupUuid).getGroup();
    }

}
