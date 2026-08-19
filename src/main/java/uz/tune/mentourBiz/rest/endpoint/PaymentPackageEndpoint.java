package uz.tune.mentourBiz.rest.endpoint;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.PaymentPackage;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.Course;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.CourseStatus;
import uz.tune.mentourBiz.rest.enums.FinanceEnums;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.payload.req.ReqPaymentPackage;
import uz.tune.mentourBiz.rest.payload.res.ResPaymentPackage;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.repository.PaymentPackageRepo;
import uz.tune.mentourBiz.rest.repository.course.CourseRepo;
import uz.tune.mentourBiz.rest.repository.group.GroupRepository;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.rest.service.lessons.impl.CourseServiceImpl;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + "/finance/packages")
@RequiredArgsConstructor
public class PaymentPackageEndpoint {

    private final PaymentPackageRepo packageRepo;
    private final UserService userService;
    private final UserScopeService userScopeService;
    private final SchoolRepo schoolRepo;
    private final CourseRepo courseRepo;
    private final CourseServiceImpl courseServiceImpl;
    private final AuthToViewEntity authToViewEntity;
    private final EnrollmentRepository enrollmentRepository;
    private final GroupRepository groupRepository;

    @GetMapping
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).LIST)")
    public ResponseEntity<List<ResPaymentPackage>> list(
            @RequestParam(required = false) UUID schoolUuid,
            @RequestParam(required = false) String name) {

        // Resolve: If param is null, resolvedSchoolUuids contains all branches in Org
        Collection<UUID> resolvedSchoolUuids;
        UUID singleId = userScopeService.resolveSchoolUuid(schoolUuid);

        if (singleId != null) {
            resolvedSchoolUuids = List.of(singleId);
        } else {
            resolvedSchoolUuids = userScopeService.getAuthorizedSchoolUuids();
        }

        // find packages for ALL schools in the Org
        List<PaymentPackage> packages = packageRepo.findAllBySchool_UuidInAndName(resolvedSchoolUuids, name);

        return ResponseEntity.ok(packages.stream().map(pkg -> {
            List<Course> courses = courseRepo.findAllByPaymentPackage_UuidAndStatus(pkg.getUuid(), CourseStatus.ACTIVE);
            int courseCount = courses.size();
            long studentCount = enrollmentRepository.countActiveStudentsByPaymentPackage(pkg);
            return new ResPaymentPackage(pkg, courseCount, studentCount, courses);
        }).toList());
    }

    @PostMapping("/{uuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).UPSERT)")
    public ResponseEntity<ResponseMessage> upsert(
            @PathVariable UUID uuid,
            @RequestBody ReqPaymentPackage pkg) {

        User user = userService.getCurrentUser();
        PaymentPackage paymentPackage = packageRepo.findByUuid(uuid).orElse(new PaymentPackage());

        UUID resolvedSchoolUuid = userScopeService.resolveSchoolUuid(pkg.getSchoolUuid());

        if (resolvedSchoolUuid == null) {
            if (!user.getRole().equals(UserRole.SYS_ADMIN)) {
                throw new ValidationException("A specific school branch must be selected for this package.");
            }
            paymentPackage.setSchool(null);
        } else {
            School school = schoolRepo.findByUuid(resolvedSchoolUuid)
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));

            authToViewEntity.authorizeActionUponSchoolBroadAccess(school);
            paymentPackage.setSchool(school);
        }

        paymentPackage.setCurrency(pkg.getCurrency() != null ? pkg.getCurrency() : FinanceEnums.FinanceCurrency.UZS);
        paymentPackage.setName(pkg.getName());
        paymentPackage.setPrice(pkg.getPrice());
        paymentPackage.setPaymentDueDate(pkg.getPaymentDueDate());

        packageRepo.save(paymentPackage);

        if(CoreUtils.isPresent(pkg.getCourseUuid())){
            List<Course> courses = new ArrayList<>();
            for (UUID cUuid : pkg.getCourseUuid()) {
                Course course = courseRepo.findByUuid(cUuid)
                        .orElseThrow(() -> new EntityNotFoundException(MessageKey.COURSE_NOT_FOUND.getKey()));

                authToViewEntity.authorizeUponCourse(user, course);
                course.setPaymentPackage(paymentPackage);
                courses.add(course);
            }
            courseRepo.saveAll(courses);
        }

        return ResponseEntity.ok(new ResponseMessage("Package successfully saved."));
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).DELETE)")
    public ResponseEntity<ResponseMessage> delete(@PathVariable UUID uuid) {
        User user = userService.getCurrentUser();

        PaymentPackage paymentPackage = packageRepo.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.ORDER_NOT_FOUND.getKey()));

        if (paymentPackage.getSchool() != null) {
            authToViewEntity.authorizeActionUponSchoolBroadAccess(paymentPackage.getSchool());
        } else if (!user.getRole().equals(UserRole.SYS_ADMIN)) {
            throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
        }

        List<Course> linkedCourses = courseRepo.findAllByPaymentPackage_UuidAndStatus(uuid, CourseStatus.ACTIVE);

        for (Course course : linkedCourses) {
            course.setPaymentPackage(null);
        }
        courseRepo.saveAll(linkedCourses);

        packageRepo.delete(paymentPackage);

        return ResponseEntity.ok(new ResponseMessage("Package deleted and unlinked from " + linkedCourses.size() + " courses."));
    }
}