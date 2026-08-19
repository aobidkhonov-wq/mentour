package uz.tune.mentourBiz.rest.service;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PaymentRequiredException;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.*;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.Course;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.StudentDiscount;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.*;
import uz.tune.mentourBiz.rest.payload.req.ReqBulkInitialCharge;
import uz.tune.mentourBiz.rest.payload.req.ReqBulkTopUp;
import uz.tune.mentourBiz.rest.payload.res.*;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;
import uz.tune.mentourBiz.rest.payload.res.student.ResStudentDiscount;
import uz.tune.mentourBiz.rest.repository.FinanceTransactionRepo;
import uz.tune.mentourBiz.rest.repository.OrganizationRepository;
import uz.tune.mentourBiz.rest.repository.PaymentPackageRepo;
import uz.tune.mentourBiz.rest.repository.SchoolDirectorRepo;
import uz.tune.mentourBiz.rest.repository.course.CourseRepo;
import uz.tune.mentourBiz.rest.repository.group.GroupRepository;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.rest.service.student.StudentDiscountService;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FinanceService {

    private final StudentRepo studentRepo;
    private final FinanceTransactionRepo transactionRepo;
    private final UserScopeService userScopeService;
    private final UserService userService;
    private final AuthToViewEntity authToViewEntity;
    private final CourseRepo courseRepo;
    private final SchoolRepo schoolRepo;
    private final PaymentPackageRepo paymentPackageRepo;
    private final OrganizationRepository organizationRepository;
    private final SchoolDirectorRepo schoolDirectorRepo;
    private final StudentDiscountService studentDiscountService;
    private final GroupRepository groupRepository;
    private final EnrollmentRepository enrollmentRepository;

    private Instant getStartOfCurrentMonth() {
        return LocalDate.now().minusYears(2).withDayOfMonth(1).atStartOfDay(ZoneId.of("Asia/Tashkent")).toInstant();
    }

    @Transactional
    public Boolean activate(UUID orgUuid) {
        User user = userService.getCurrentUser();
        Organization organization;
        List<UUID> schoolUuids;
        if (user.getRole().equals(UserRole.SYS_ADMIN)) {
            organization = organizationRepository.findByUuid(orgUuid)
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.ORG_NOT_FOUND.getKey()));
            schoolUuids = organization.getSchools().stream().map(School::getUuid).toList();
        } else {
            SchoolDirector schoolDirector = schoolDirectorRepo.findByUser(user)
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.DIRECTOR_NOT_FOUND.getKey()));
            organization = schoolDirector.getOrganization();
            schoolUuids = organization.getSchools().stream().map(School::getUuid).toList();
        }
        List<School> schools = schoolRepo.findAllByUuidIn(schoolUuids);
        List<School> toUpd = new ArrayList<>();
        List<Student> students = new ArrayList<>();
        for (School school : schools) {
            if (school.isPaymentActive()) continue;
            school.setPaymentActive(true);
            school.setPaymentActivatedTime(Instant.now());
            school.setPaymentActivatedBy(userService.getCurrentUser());
            toUpd.add(school);

            studentRepo.findAllBySchool_Uuid(school.getUuid(), Pageable.unpaged()).forEach(s -> {
                s.setPaymentActivated(true);
                students.add(s);
            });
        }
        schoolRepo.saveAll(toUpd);
        studentRepo.saveAll(students);
        return true;
    }

//    @Transactional(readOnly = true)
//    public Page<ResStudentFinanceList> getOrgFinanceDashboard(UUID orgUuid, FinanceEnums.FinanceStatus status, boolean activeOnly, Pageable pageable) {
//        String statusName = (status != null) ? status.name() : null;
//        organizationRepository.findByUuid(orgUuid).orElseThrow(() -> new EntityNotFoundException(MessageKey.ORG_NOT_FOUND.getKey()));
//
//        return studentRepo.findFinanceDashboardFilteredByOrg(orgUuid, statusName, activeOnly, pageable)
//                .map(s -> mapToFinanceDto(s, s.getSchool()));
//    }
//
//    @Transactional(readOnly = true)
//    public ResOrganizationFinanceSummary getOrganizationFinanceSummary(UUID organizationUuid, UUID groupUuid, Instant fromDate, Instant toDate, boolean activeOnly) {
//        User user = userService.getCurrentUser();
//        Organization org = (user.getRole().equals(UserRole.SYS_ADMIN) && organizationUuid != null) ?
//                organizationRepository.findByUuid(organizationUuid).orElseThrow() :
//                schoolDirectorRepo.findByUser(user).orElseThrow().getOrganization();
//
//        List<UUID> activeSchoolUuids = org.getSchools().stream()
//                .filter(School::isPaymentActive)
//                .map(School::getUuid).toList();
//
//        if (activeSchoolUuids.isEmpty()) return ResOrganizationFinanceSummary.builder().schoolSummaries(new ArrayList<>()).build();
//
//        ZoneId zone = ZoneId.of("Asia/Tashkent");
//        Instant now = Instant.now();
//        Instant startOfCurrMonth = LocalDate.now(zone).withDayOfMonth(1).atStartOfDay(zone).toInstant();
//
//        Instant effFrom = (fromDate != null) ? fromDate : startOfCurrMonth;
//        Instant effTo = (toDate != null) ? toDate : now;
//
//        long totalExpected = Math.abs(transactionRepo.sumFilteredTransactions(activeSchoolUuids, FinanceEnums.FinanceTransactionType.CHARGE, null, groupUuid, effFrom, effTo, activeOnly));
//        long totalCollected = transactionRepo.sumFilteredTransactions(activeSchoolUuids, FinanceEnums.FinanceTransactionType.PAYMENT, null, groupUuid, effFrom, effTo, activeOnly);
//        long totalBonus = transactionRepo.sumFilteredTransactions(activeSchoolUuids, FinanceEnums.FinanceTransactionType.PAYMENT, FinanceEnums.PaymentMethod.BONUS, groupUuid, effFrom, effTo, activeOnly);
//        long totalOutstanding = studentRepo.sumOutstandingBalanceFiltered(activeSchoolUuids, groupUuid, activeOnly);
//
//        // 2. Org-wide Growth Rate (Last Month Full vs Current Month to Date)
//        Double orgGrowthRate = null;
//        if (fromDate == null && toDate == null) {
//            Instant startOfPrevMonth = LocalDate.now(zone).minusMonths(1).withDayOfMonth(1).atStartOfDay(zone).toInstant();
//            Instant endOfPrevMonth = LocalDate.now(zone).withDayOfMonth(1).atStartOfDay(zone).minusNanos(1000).toInstant();
//
//            long cMonth = transactionRepo.sumFilteredTransactions(activeSchoolUuids, FinanceEnums.FinanceTransactionType.PAYMENT, null, groupUuid, startOfCurrMonth, now, activeOnly);
//            long pMonth = transactionRepo.sumFilteredTransactions(activeSchoolUuids, FinanceEnums.FinanceTransactionType.PAYMENT, null, groupUuid, startOfPrevMonth, endOfPrevMonth, activeOnly);
//
//            if (pMonth > 0) orgGrowthRate = Math.round(((double) (cMonth - pMonth) / pMonth) * 1000.0) / 10.0;
//            else orgGrowthRate = (cMonth > 0) ? 100.0 : 0.0;
//        }
//
//        // 3. School breakdowns
//        List<ResFinanceSummary> schoolSummaries = org.getSchools().stream()
//                .filter(School::isPaymentActive)
//                .map(s -> getFinanceSummary(s.getUuid(), groupUuid, fromDate, toDate, activeOnly))
//                .toList();
//
//        return ResOrganizationFinanceSummary.builder()
//                .totalExpectedRevenue(totalExpected)
//                .collectedRevenue(totalCollected)
//                .totalBonusRevenue(totalBonus)
//                .outstandingBalance(Math.abs(totalOutstanding))
//                .collectionRate(totalExpected > 0 ? (double) totalCollected / totalExpected * 100 : 0)
//                .growthRate(orgGrowthRate)
//                .schoolSummaries(schoolSummaries)
//                .build();
//    }

    @Transactional(readOnly = true)
    public ResFinanceSummary getFinanceSummary(UUID schoolUuid, UUID groupUuid, UUID courseUuid, UUID teacherUuid, Instant fromDate, Instant toDate, boolean activeOnly) {
        User user = userService.getCurrentUser();
        UUID resolvedUuid = userScopeService.resolveSchoolUuid(schoolUuid);

        Collection<UUID> schoolUuids;
        String label;

        if (resolvedUuid != null) {
            School school = schoolRepo.findByUuid(resolvedUuid).orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));
            if (!school.isPaymentActive()) {
                throw new PaymentRequiredException(MessageKey.BILLING_INACTIVE.getKey());
            }
            schoolUuids = List.of(resolvedUuid);
            label = school.getName();
        } else {
            List<UUID> authorizedIds = userScopeService.getAuthorizedSchoolUuids();
            schoolUuids = schoolRepo.findAllByUuidIn(authorizedIds).stream()
                    .filter(School::isPaymentActive)
                    .map(School::getUuid).toList();

            if (schoolUuids.isEmpty()) return ResFinanceSummary.builder().schoolName("No Active Billing Branches").build();
            label = (user.getRole() == UserRole.SYS_ADMIN) ? "System Total" : "Organization Total";
        }

        ZoneId zone = ZoneId.of("Asia/Tashkent");
        Instant now = Instant.now();
        Instant startOfCurrMonth = LocalDate.now(zone).withDayOfMonth(1).atStartOfDay(zone).toInstant();

        Instant effectiveFrom = (fromDate != null) ? fromDate : startOfCurrMonth;
        Instant effectiveTo = (toDate != null) ? toDate : now;

        // A. Total Charges (Passing courseUuid and teacherUuid)
        long totalCharges = Math.abs(transactionRepo.sumFilteredTransactions(
                schoolUuids, FinanceEnums.FinanceTransactionType.CHARGE, null,
                groupUuid, courseUuid, teacherUuid, effectiveFrom, effectiveTo, activeOnly, false));

        // B. Total Bonuses
        long totalBonus = transactionRepo.sumFilteredTransactions(
                schoolUuids, FinanceEnums.FinanceTransactionType.PAYMENT, FinanceEnums.PaymentMethod.BONUS,
                groupUuid, courseUuid, teacherUuid, effectiveFrom, effectiveTo, activeOnly, false);

        // C. Collected Revenue (Exclude Bonuses)
        long collected = transactionRepo.sumFilteredTransactions(
                schoolUuids, FinanceEnums.FinanceTransactionType.PAYMENT, null,
                groupUuid, courseUuid, teacherUuid, effectiveFrom, effectiveTo, activeOnly, true);

        // D. Expected Revenue = Total Charges minus applied Bonuses
        long expected = totalCharges - totalBonus;

        // D. Outstanding Balance
        long outstanding = studentRepo.sumOutstandingBalanceFiltered(schoolUuids, groupUuid, courseUuid, teacherUuid, activeOnly);

        // E. Growth Rate (Simplified for current filtered context)
        Double growthRate = null;
        if (fromDate == null && toDate == null) {
            Instant startOfPrevMonth = LocalDate.now(zone).minusMonths(1).withDayOfMonth(1).atStartOfDay(zone).toInstant();
            Instant endOfPrevMonth = LocalDate.now(zone).withDayOfMonth(1).atStartOfDay(zone).minusNanos(1000000).toInstant();

            long currentMonthRev = transactionRepo.sumFilteredTransactions(
                    schoolUuids, FinanceEnums.FinanceTransactionType.PAYMENT, null, groupUuid, courseUuid, teacherUuid, startOfCurrMonth, now, activeOnly, true);
            long prevMonthRev = transactionRepo.sumFilteredTransactions(
                    schoolUuids, FinanceEnums.FinanceTransactionType.PAYMENT, null, groupUuid, courseUuid, teacherUuid, startOfPrevMonth, endOfPrevMonth, activeOnly, true);

            if (prevMonthRev > 0) {
                growthRate = Math.round(((double) (currentMonthRev - prevMonthRev) / prevMonthRev) * 1000.0) / 10.0;
            } else {
                growthRate = (currentMonthRev > 0) ? 100.0 : 0.0;
            }
        }

        return ResFinanceSummary.builder()
                .schoolName(label)
                .schoolUuid(resolvedUuid)
                .totalExpectedRevenue(expected)
                .collectedRevenue(collected)
                .totalBonusRevenue(totalBonus)
                .outstandingBalance(Math.abs(outstanding))
                .collectionRate(expected > 0 ? (double) collected / expected * 100 : (collected > 0 ? 100.0 : 0.0))
                .growthRate(growthRate)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ResStudentFinanceDashboard> getFinanceDashboard(
            UUID schoolUuid, UUID courseUuid, String studentName, UUID teacherUuid,
            FinanceEnums.FinanceStatus status, Instant fromDate, Instant toDate, // Added toDate
            UUID packageUuid, UUID groupUuid, boolean activeOnly, boolean onlyOverdue, Pageable pageable) {

        User user = userService.getCurrentUser();

        UUID resolvedUuid = userScopeService.resolveSchoolUuid(schoolUuid);
        Collection<UUID> schoolUuids = (resolvedUuid != null) ?
                List.of(resolvedUuid) : userScopeService.getAuthorizedSchoolUuids();

        String statusName = (status != null) ? status.name() : null;
        Instant effectiveFrom = (fromDate != null) ? fromDate : null;
        Instant effectiveTo = (toDate != null) ? toDate : null;

        Page<Student> page = studentRepo.findFinanceDashboardFiltered(
                schoolUuids,
                courseUuid,
                studentName,
                teacherUuid,
                statusName,
                packageUuid,
                groupUuid,
                effectiveFrom,
                effectiveTo,
                activeOnly,
                onlyOverdue,
                pageable
        );

        // One query for the whole page rather than a discount lookup per student.
        Map<UUID, List<StudentDiscount>> discounts = studentDiscountService.activeDiscountsFor(
                page.getContent().stream().map(Student::getUuid).toList());

        return page.map(s -> mapToFinanceDto(s, groupUuid, discounts.getOrDefault(s.getUuid(), List.of())));
    }

    private ResStudentFinanceDashboard mapToFinanceDto(Student s, UUID groupUuid, List<StudentDiscount> discounts) {
        List<Enrollment> relevantEnrollments = s.getEnrollments().stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ONGOING)
                .filter(e -> groupUuid == null
                        || (e.getGroup() != null && groupUuid.equals(e.getGroup().getUuid())))
                .toList();

        // Each group carries the billing plan of its own ongoing enrollment, so a multi-group
        // student never shows a package detached from the group it actually belongs to.
        List<ResStudentFinanceDashboard.GroupInfo> groups = relevantEnrollments.stream()
                .filter(e -> e.getGroup() != null)
                .map(e -> {
                    ResStudentFinanceDashboard.GroupInfo info = new ResStudentFinanceDashboard.GroupInfo(e);
                    info.applyDiscounts(discounts);
                    return info;
                })
                .toList();

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Tashkent"));
        ResStudentFinanceDashboard dto = new ResStudentFinanceDashboard();
        dto.setDiscounts(discounts.stream().map(d -> new ResStudentDiscount(d, today)).toList());
        dto.setId(s.getUuid());
        dto.setGroups(groups);
        dto.setAttachment(s.getUser().getAttachment() != null ? new ResAttachment(s.getUser().getAttachment()) : null);
        dto.setStudentName(s.getUser().getFirstName() + " " + s.getUser().getLastName());
        dto.setUserName(s.getUser().getUsername());
        dto.setBalance(s.getCurrentBalance());
        dto.setUserStatus(s.getUser().getStatus());
        dto.setStatus(calculateStatus(s));

        relevantEnrollments.stream()
                .map(Enrollment::getGroup)
                .filter(g -> g != null && g.getTeacher() != null)
                .findFirst()
                .ifPresent(g -> {
                    User teacherUser = g.getTeacher().getUser();
                    dto.setTeacherFullName(teacherUser.getFirstName() + " " + teacherUser.getLastName());
                });
        return dto;
    }

    @Transactional(readOnly = true)
    public ResFinanceHistoryWrapper getFinanceHistory(
            UUID schoolUuid, FinanceEnums.PaymentMethod method,
            Instant from, Instant to, Pageable pageable) {

        User user = userService.getCurrentUser();

        UUID resolvedUuid = userScopeService.resolveSchoolUuid(schoolUuid);
        Collection<UUID> schoolUuids;
        if (resolvedUuid != null) {
            schoolUuids = List.of(resolvedUuid);
        } else {
            schoolUuids = userScopeService.getAuthorizedSchoolUuids();
        }

        // 2. Setup Date Filters (Default to current month if from is missing)
        Instant effectiveFrom = (from != null) ? from : getStartOfCurrentMonth();
        Instant effectiveTo = (to != null) ? to : Instant.now();

        Page<ResFinanceTransaction> page = transactionRepo.findHistoryMulti(
                        schoolUuids, method, effectiveFrom, effectiveTo, pageable)
                .map(ResFinanceTransaction::new);

        Long totalRevenue = transactionRepo.sumRevenueInRangeFilteredMulti(
                schoolUuids, effectiveFrom, effectiveTo, true);

        return new ResFinanceHistoryWrapper(page, totalRevenue);
    }

    /**
     * Paginated history filtered by transaction {@code type} (chosen by the caller). Pass CHARGE to
     * see money auto-deducted from students (billing-plan renewals, per-lesson/initial charges),
     * PAYMENT for top-ups, ADJUSTMENT for refunds/write-offs, or null for every movement. Kept
     * separate from {@link #getFinanceHistory}, which is locked to PAYMENT. {@code total} is the
     * signed sum of amounts in range (CHARGE negative, PAYMENT positive).
     */
    @Transactional(readOnly = true)
    public ResFinanceHistoryWrapper getHistoryByType(
            UUID schoolUuid, FinanceEnums.FinanceTransactionType type, FinanceEnums.PaymentMethod method,
            Instant from, Instant to, Pageable pageable) {

        UUID resolvedUuid = userScopeService.resolveSchoolUuid(schoolUuid);
        Collection<UUID> schoolUuids = (resolvedUuid != null)
                ? List.of(resolvedUuid)
                : userScopeService.getAuthorizedSchoolUuids();

        Instant effectiveFrom = (from != null) ? from : getStartOfCurrentMonth();
        Instant effectiveTo = (to != null) ? to : Instant.now();

        Page<ResFinanceTransaction> page = transactionRepo.findHistoryByTypeMulti(
                        schoolUuids, type, method, effectiveFrom, effectiveTo, pageable)
                .map(ResFinanceTransaction::new);

        Long total = transactionRepo.sumHistoryByTypeMulti(
                schoolUuids, type, method, effectiveFrom, effectiveTo);

        return new ResFinanceHistoryWrapper(page, total);
    }

    /**
     * Soft-deletes a finance transaction that shows up in the history lists and undoes its effect on
     * the student's balance.
     *
     * <p>The balance is a stored running total, so simply hiding the row would leave it wrong: a deleted
     * CHARGE has to be handed back and a deleted PAYMENT taken away again. Both are {@code balance -=
     * amount}, because charges are stored negative and payments positive. The row itself is kept (flagged
     * {@code deleted}) for audit and its {@code acceptedBy} is overwritten with whoever deleted it, so
     * the record carries the last person who acted on it. The deleted row stays visible in the charge
     * history list (flagged), but every revenue sum, payroll replay and status check filters it out with
     * {@code deleted = false}, so the reversed money is never counted again.
     */
    @Transactional
    public ResponseMessage deleteTransaction(UUID transactionUuid) {
        FinanceTransaction tx = transactionRepo.findByUuidAndDeletedFalse(transactionUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.TRANSACTION_NOT_FOUND.getKey()));

        Student student = tx.getStudent();
        authToViewEntity.authorizeActionUponStudent(student);

        if (tx.getAmount() != null) {
            student.setCurrentBalance(student.getCurrentBalance() - tx.getAmount());
            studentRepo.save(student);
        }

        tx.setDeleted(true);
        tx.setAcceptedBy(userService.getCurrentUser());
        transactionRepo.save(tx);

        return new ResponseMessage("Transaction deleted for " + student.getUser().getFirstName());
    }

    private FinanceEnums.FinanceStatus calculateStatus(Student s) {
        boolean hasBeenCharged = transactionRepo.existsByStudentAndTypeAndDeletedFalse(s, FinanceEnums.FinanceTransactionType.CHARGE);

        if (!hasBeenCharged) return FinanceEnums.FinanceStatus.NEW;

        return (s.getCurrentBalance() >= 0) ? FinanceEnums.FinanceStatus.PAID : FinanceEnums.FinanceStatus.UNPAID;
    }

    @Transactional
    public ResponseMessage batchDeactivateStudentPayments(List<UUID> studentUuids) {
        List<Student> students = studentRepo.findAllByUuidIn(studentUuids);
        User currentUser = userService.getCurrentUser();

        for (Student student : students) {
            authToViewEntity.authorizeActionUponStudent(student);
            long debt = student.getCurrentBalance();

            if (debt < 0) {
                FinanceTransaction adjustment = new FinanceTransaction();
                adjustment.setStudent(student);
                adjustment.setAmount(Math.abs(debt));
                adjustment.setType(FinanceEnums.FinanceTransactionType.ADJUSTMENT);
                adjustment.setMethod(FinanceEnums.PaymentMethod.SYSTEM_ADJUSTMENT);
                adjustment.setAcceptedBy(currentUser);
                adjustment.setNote("System Write-off: Payment deactivated for inactive student.");
                transactionRepo.save(adjustment);
            }

            student.setCurrentBalance(0L);
            student.setPaymentActivated(false);
        }

        studentRepo.saveAll(students);
        return new ResponseMessage("Successfully deactivated " + students.size() + " students and balanced their accounts.");
    }

    private void checkPaymentActive(School school) {
        if (!school.isPaymentActive()) throw new PaymentRequiredException(MessageKey.BILLING_INACTIVE.getKey());
    }

    /**
     * Registers a saved transaction at the moment the front-end supplied instead of now. The value is read as
     * Tashkent local time, matching the zone every finance report groups by. Null leaves the creation
     * timestamp alone.
     */
    private void applyPaymentDate(FinanceTransaction tx, LocalDateTime paymentDate) {
        if (paymentDate == null) return;
        transactionRepo.flush();
        transactionRepo.updateTransactionDate(tx.getId(), paymentDate.atZone(ZoneId.of("Asia/Tashkent")).toInstant());
    }

    @Transactional
    public ResponseMessage applyInitialCharge(UUID studentUuid, Long amount, String note, LocalDateTime paymentDate) {
        Student student = studentRepo.findByUuid(studentUuid).orElseThrow();
        authToViewEntity.authorizeActionUponStudent(student);
        student.setPaymentActivated(true);
        student.setCurrentBalance(student.getCurrentBalance() - amount);
        studentRepo.save(student);

        FinanceTransaction charge = new FinanceTransaction();
        charge.setStudent(student);
        charge.setAmount(-amount);
        charge.setType(FinanceEnums.FinanceTransactionType.CHARGE);
        charge.setAcceptedBy(userService.getCurrentUser());
        charge.setNote(note);
        transactionRepo.save(charge);
        applyPaymentDate(charge, paymentDate);

        return new ResponseMessage("Initial charge applied.");
    }

    /**
     * Take money in against a student's som balance.
     *
     * <p>{@code groupUuid} is optional and says which group the money was paid for. It changes nothing
     * about the balance — that is one shared wallet either way — but teacher payroll settles an
     * earmarked payment against that group's own charges first instead of the oldest charge on the
     * account. Without it a student in two groups can have a payment meant for one group credited
     * entirely to the other, because the wallet cannot tell the two apart. Leave it null for an
     * ordinary top-up and the existing oldest-first behaviour applies unchanged.
     */
    @Transactional
    public ResponseMessage topUp(UUID studentUuid, Long amount, FinanceEnums.PaymentMethod method, String note,
                                 LocalDateTime paymentDate, UUID groupUuid) {
        Student student = studentRepo.findByUuid(studentUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
        authToViewEntity.authorizeActionUponStudent(student);

        checkPaymentActive(student.getSchool());

        // Refuse a group the student was never in: the earmark decides which teacher is credited with
        // the cash, so a wrong uuid here quietly moves revenue onto somebody else's payslip.
        Group group = null;
        if (groupUuid != null) {
            group = groupRepository.findByUuid(groupUuid)
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.GROUP_NOT_FOUND.getKey()));
            if (!enrollmentRepository.existsByStudent_UuidAndGroup_Uuid(student.getUuid(), groupUuid)) {
                throw new ValidationException("The student has no enrollment in the group this payment names.");
            }
        }

        student.setCurrentBalance(student.getCurrentBalance() + amount);
        if (student.getCurrentBalance() >= 0 && student.getUser().getStatus() == UserStatus.FROZEN) {
            student.getUser().setStatus(UserStatus.ACTIVE);
        }

        studentRepo.save(student);

        FinanceTransaction tx = new FinanceTransaction();
        tx.setStudent(student);
        tx.setGroup(group);
        tx.setAmount(amount);
        tx.setType(FinanceEnums.FinanceTransactionType.PAYMENT);
        tx.setMethod(method);
        tx.setAcceptedBy(userService.getCurrentUser());
        tx.setNote(note);
        transactionRepo.save(tx);
        applyPaymentDate(tx, paymentDate);

        return new ResponseMessage("Payment added for " + student.getUser().getFirstName());
    }

    /**
     * The legacy PaymentPackage monthly charge. Like every recurring tuition charge it honours the
     * student's discount: they are billed the package price less whatever they are let off.
     */
    @Transactional
    public ResponseMessage applyMonthlyCharge(Student student, PaymentPackage pkg, String note) {
        if (pkg == null || pkg.getPrice() <= 0) return null;

        long discount = studentDiscountService.discountFor(student, pkg.getPrice());
        long payable = pkg.getPrice() - discount;

        student.setCurrentBalance(student.getCurrentBalance() - payable);
        FinanceTransaction charge = new FinanceTransaction();
        charge.setStudent(student);
        charge.setAmount(-payable);
        charge.setDiscountAmount(discount);
        charge.setType(FinanceEnums.FinanceTransactionType.CHARGE);
        charge.setNote(discount > 0 ? note + " — discount " + discount + " of " + pkg.getPrice() : note);
        transactionRepo.save(charge);
        studentRepo.save(student);
        return new ResponseMessage("Charged.");
    }

    @Transactional
    public ResponseMessage bulkTopUp(ReqBulkTopUp request) {

        User currentUser = userService.getCurrentUser();

        List<Student> students = studentRepo.findAllByUuidIn(request.studentUuids());
        for (Student s : students) {
            authToViewEntity.authorizeActionUponStudent(s);
            checkPaymentActive(s.getSchool());
            s.setCurrentBalance(s.getCurrentBalance() + request.amount());
            FinanceTransaction tx = new FinanceTransaction();
            tx.setStudent(s); tx.setAmount(request.amount()); tx.setType(FinanceEnums.FinanceTransactionType.PAYMENT);
            tx.setMethod(request.method()); tx.setAcceptedBy(userService.getCurrentUser()); tx.setNote(request.note());
            transactionRepo.save(tx);
            applyPaymentDate(tx, request.paymentDate());
        }

        studentRepo.saveAll(students);
        return new ResponseMessage("Bulk top-up complete.");
    }

    @Transactional
    public ResponseMessage bulkInitialCharge(ReqBulkInitialCharge request) {

        User currentUser = userService.getCurrentUser();

        List<Student> students = studentRepo.findAllByUuidIn(request.studentUuids());
        for (Student s : students) {
            authToViewEntity.authorizeActionUponStudent(s);
            s.setPaymentActivated(true);
            s.setCurrentBalance(s.getCurrentBalance() - request.amount());
            FinanceTransaction charge = new FinanceTransaction();
            charge.setStudent(s); charge.setAmount(-request.amount()); charge.setType(FinanceEnums.FinanceTransactionType.CHARGE);
            charge.setAcceptedBy(userService.getCurrentUser()); charge.setNote(request.note());
            transactionRepo.save(charge);
        }

        studentRepo.saveAll(students);
        return new ResponseMessage("Bulk charge complete.");
    }

    public void handleEnrollmentCharge(Student student, PaymentPackage pkg) {
        if (pkg == null || pkg.getPaymentDueDate() == null) return;
        if (java.time.LocalDate.now().getDayOfMonth() == pkg.getPaymentDueDate()) applyMonthlyCharge(student, pkg, "Enrollment charge");
    }
}