package uz.tune.mentourBiz.rest.service.util.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.repository.group.GroupRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepository;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.user.UserRepo;
import uz.tune.mentourBiz.rest.service.group.enrollment.StudentEnrollmentHelper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Row-level persistence for the student Excel import. Each row is written in its own transaction so
 * a broken row (constraint violation, missing group, enrollment error) is skipped without discarding
 * the rows imported before it. The caller must therefore NOT run the import inside a transaction.
 */
@Service
@RequiredArgsConstructor
public class StudentExcelImportHelper {

    private final UserRepo userRepo;
    private final StudentRepo studentRepo;
    private final SchoolRepository schoolRepository;
    private final GroupRepository groupRepository;
    private final StudentEnrollmentHelper studentEnrollmentHelper;

    /**
     * Schools the student limit is counted over: the whole organization when the school belongs to
     * one, otherwise just the school itself. Resolved in a transaction because both associations are lazy.
     */
    @Transactional(readOnly = true)
    public List<UUID> resolveLimitScopeSchoolUuids(UUID schoolUuid) {
        School school = schoolRepository.findByUuid(schoolUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));
        return (school.getOrganization() != null)
                ? school.getOrganization().getSchools().stream().map(School::getUuid).collect(Collectors.toList())
                : List.of(school.getUuid());
    }

    /**
     * Persists one imported row: user account, student profile and the optional group enrollment.
     * School and group are re-loaded here so they are managed by this transaction's session.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void importStudent(User userAccount, UUID schoolUuid, UUID groupUuid) {
        School school = schoolRepository.findByUuid(schoolUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));

        User savedUser = userRepo.save(userAccount);

        Student student = new Student();
        student.setUser(savedUser);
        student.setSchool(school);
        Student savedStudent = studentRepo.save(student);

        if (groupUuid != null) {
            Group group = groupRepository.findByUuid(groupUuid)
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.GROUP_NOT_FOUND.getKey()));
            studentEnrollmentHelper.enrollToGroup(savedStudent, group);
        }
    }
}
