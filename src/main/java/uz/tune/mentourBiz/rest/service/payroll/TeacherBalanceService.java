package uz.tune.mentourBiz.rest.service.payroll;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.finance.SchoolExpense;
import uz.tune.mentourBiz.rest.domain.payroll.TeacherBalanceEntry;
import uz.tune.mentourBiz.rest.domain.payroll.TeacherPayment;
import uz.tune.mentourBiz.rest.domain.payroll.TeacherPaymentAllocation;
import uz.tune.mentourBiz.rest.domain.payroll.TeacherPayslip;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.enums.PayrollEnums;
import uz.tune.mentourBiz.rest.payload.req.payroll.ReqTeacherPayment;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.payroll.ResBalanceEntry;
import uz.tune.mentourBiz.rest.payload.res.payroll.ResTeacherBalance;
import uz.tune.mentourBiz.rest.payload.res.payroll.ResTeacherBalanceRow;
import uz.tune.mentourBiz.rest.payload.res.payroll.ResTeacherPayment;
import uz.tune.mentourBiz.rest.repository.payroll.TeacherBalanceEntryRepository;
import uz.tune.mentourBiz.rest.repository.payroll.TeacherPaymentAllocationRepository;
import uz.tune.mentourBiz.rest.repository.payroll.TeacherPaymentRepository;
import uz.tune.mentourBiz.rest.repository.payroll.TeacherPayslipRepository;
import uz.tune.mentourBiz.rest.repository.user.TeacherRepository;
import uz.tune.mentourBiz.rest.service.finance.SchoolExpenseService;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A teacher's balance: what the school has agreed to pay them and has not handed over yet.
 *
 * <p>Two things move it, and only two. Approving a payslip credits its net pay — that is the moment the
 * month stops being a calculation and becomes a debt. Paying the teacher debits it. Nothing resets at a
 * month boundary, so a July remainder is still sitting there when August is approved on top of it.
 *
 * <p>A payment is never larger than the balance. That single rule is what keeps the ledger and the
 * payslips telling the same story: the balance always equals the sum of what the open payslips still
 * owe, so an amount that fits the balance is always allocatable, and an advance can only be paid once
 * the month it draws on has been approved.
 */
@Service
@RequiredArgsConstructor
public class TeacherBalanceService {

    private static final ZoneId UZ_ZONE = ZoneId.of("Asia/Tashkent");

    private final TeacherBalanceEntryRepository balanceEntryRepository;
    private final TeacherPaymentRepository paymentRepository;
    private final TeacherPaymentAllocationRepository allocationRepository;
    private final TeacherPayslipRepository payslipRepository;
    private final TeacherRepository teacherRepository;
    private final SchoolExpenseService expenseService;
    private final UserScopeService userScopeService;
    private final UserService userService;

    // ---- Reading ---------------------------------------------------------------------------------

    /** The balances screen: every teacher in scope and what they are owed, largest first. */
    @Transactional(readOnly = true)
    public Page<ResTeacherBalanceRow> list(String search, Pageable pageable) {
        List<UUID> schools = userScopeService.getAuthorizedSchoolUuids();
        if (schools != null && schools.isEmpty()) return Page.empty(pageable);

        // The ordering is done in the query rather than over the fetched page: sorting afterwards
        // would only order each page within itself, which on a "who do we owe" screen is worse than
        // not sorting at all.
        Page<Object[]> page = balanceEntryRepository.findBalancesOrdered(
                schools, blankToNull(search), pageable);
        if (page.isEmpty()) return new PageImpl<>(List.of(), pageable, page.getTotalElements());

        List<UUID> uuids = page.getContent().stream().map(row -> (UUID) row[0]).toList();
        Map<UUID, Long> openCounts = toLongMap(payslipRepository.countOpenByTeacher(uuids));

        List<ResTeacherBalanceRow> rows = new ArrayList<>();
        for (Object[] row : page.getContent()) {
            UUID uuid = (UUID) row[0];
            String name = joinName((String) row[1], (String) row[2]);
            rows.add(ResTeacherBalanceRow.builder()
                    .teacherUuid(uuid)
                    .teacherName(name)
                    .teacherInitials(TeacherPayslipService.initials(name))
                    .balance(((Number) row[3]).longValue())
                    .openPeriodCount(openCounts.getOrDefault(uuid, 0L))
                    .build());
        }
        return new PageImpl<>(rows, pageable, page.getTotalElements());
    }

    /** One teacher's balance with the months behind it. */
    @Transactional(readOnly = true)
    public ResTeacherBalance detail(UUID teacherUuid) {
        Teacher teacher = loadTeacherInScope(teacherUuid);

        long accrued = 0L;
        long paid = 0L;
        for (Object[] row : balanceEntryRepository.sumByType(teacherUuid)) {
            PayrollEnums.BalanceEntryType type = (PayrollEnums.BalanceEntryType) row[0];
            long sum = ((Number) row[1]).longValue();
            // REVERSAL is a cancelled accrual, so it belongs against the accrued total rather than in
            // a bucket of its own; PAYMENT is stored negative and is read as an amount here.
            if (type == PayrollEnums.BalanceEntryType.PAYMENT) {
                paid += -sum;
            } else {
                accrued += sum;
            }
        }

        List<ResTeacherBalance.OpenPeriod> openPeriods = new ArrayList<>();
        long balance = 0L;
        for (TeacherPayslip payslip : payslipRepository.findOpenForTeacher(teacherUuid)) {
            long netPay = nz(payslip.getNetPay());
            long alreadyPaid = allocationRepository.paidAmountOf(payslip.getUuid());
            long remaining = netPay - alreadyPaid;
            if (remaining <= 0) continue;
            balance += remaining;
            openPeriods.add(ResTeacherBalance.OpenPeriod.builder()
                    .payslipUuid(payslip.getUuid())
                    .period(periodOf(payslip))
                    .netPay(netPay)
                    .paidAmount(alreadyPaid)
                    .remainingAmount(remaining)
                    .status(payslip.getStatus())
                    .build());
        }

        TeacherPayment last = paymentRepository.findWithFilters(
                        null, List.of(teacherUuid), null, null, null,
                        org.springframework.data.domain.PageRequest.of(0, 1))
                .getContent().stream().findFirst().orElse(null);

        String name = TeacherPayslipService.userName(teacher.getUser());
        return ResTeacherBalance.builder()
                .teacherUuid(teacherUuid)
                .teacherName(name)
                .teacherInitials(TeacherPayslipService.initials(name))
                .schoolUuid(teacher.getSchool() != null ? teacher.getSchool().getUuid() : null)
                .balance(balance)
                .totalAccrued(accrued)
                .totalPaid(paid)
                .openPeriods(openPeriods)
                .lastPaymentDate(last != null ? last.getPaymentDate() : null)
                .lastPaymentAmount(last != null ? nz(last.getAmount()) : null)
                .build();
    }

    /** The balance history: every accrual, payment and reversal, newest first. */
    @Transactional(readOnly = true)
    public Page<ResBalanceEntry> history(UUID teacherUuid, Pageable pageable) {
        loadTeacherInScope(teacherUuid);
        return balanceEntryRepository
                .findAllByTeacher_User_UuidOrderByOccurredAtDescIdDesc(teacherUuid, pageable)
                .map(TeacherBalanceService::toEntryResponse);
    }

    /** The payments feed, across teachers or narrowed to one. */
    @Transactional(readOnly = true)
    public Page<ResTeacherPayment> payments(UUID teacherUuid,
                                            PayrollEnums.TeacherPaymentType type,
                                            LocalDate fromDate,
                                            LocalDate toDate,
                                            Pageable pageable) {
        return paymentRepository.findWithFilters(
                        userScopeService.getAuthorizedSchoolUuids(),
                        teacherUuid != null ? List.of(teacherUuid) : null,
                        type, fromDate, toDate, pageable)
                .map(payment -> toPaymentResponse(payment, null));
    }

    // ---- Paying ----------------------------------------------------------------------------------

    /**
     * Hand money to a teacher out of their balance, settling the oldest open month first.
     *
     * <p>Refuses anything above the balance: an advance is an early instalment of pay the school has
     * already committed to, not a loan, so there has to be an approved month behind it.
     */
    @Transactional
    public ResTeacherPayment pay(UUID teacherUuid, ReqTeacherPayment req) {
        Teacher teacher = loadTeacherInScope(teacherUuid);
        // Serialises concurrent payments to the same teacher; see the repository method.
        teacherRepository.findByUserUuidForUpdate(teacherUuid);

        List<TeacherPayslip> open = payslipRepository.findOpenForTeacher(teacherUuid);
        long balance = outstandingOf(open);
        long amount = resolveAmount(req, balance);

        if (amount > balance) {
            throw new ValidationException(String.format(
                    "Amount %d exceeds the balance of %d. Approve the payslip first, or pay at most the balance.",
                    amount, balance));
        }
        return record(teacher, open, amount, req, balance);
    }

    /**
     * Pay off one month specifically. The generic {@link #pay} always starts with the oldest open
     * month, which is right when an admin says "give Aziz 2 000 000" and wrong when they are looking at
     * one payslip and settling it — that money has to land on the month they are looking at.
     */
    @Transactional
    public ResTeacherPayment payPayslip(TeacherPayslip payslip, ReqTeacherPayment req) {
        Teacher teacher = payslip.getTeacher();
        if (teacher == null || teacher.getUser() == null) {
            throw new ValidationException("This payslip has no teacher attached.");
        }
        if (payslip.getStatus() != PayrollEnums.PayslipStatus.APPROVED
                && payslip.getStatus() != PayrollEnums.PayslipStatus.PARTIALLY_PAID) {
            // Approval is what puts the money on the balance. Paying an unapproved month would hand
            // over som that was never credited and push the balance below what the ledger says.
            throw new ValidationException("A payslip must be approved before it can be paid.");
        }
        UUID teacherUuid = teacher.getUser().getUuid();
        teacherRepository.findByUserUuidForUpdate(teacherUuid);

        long remaining = nz(payslip.getNetPay()) - allocationRepository.paidAmountOf(payslip.getUuid());
        if (remaining <= 0) {
            throw new ValidationException("This payslip has nothing left to pay.");
        }
        long amount = resolveAmount(req, remaining);
        if (amount > remaining) {
            throw new ValidationException(String.format(
                    "Amount %d exceeds the %d still owing on this payslip.", amount, remaining));
        }
        return record(teacher, List.of(payslip), amount, req, remaining);
    }

    /**
     * Undo a payment that should not have been made. The amount goes back on the balance and the
     * payslips it had settled reopen; the payment row and its expense stay as deleted for audit.
     */
    @Transactional
    public ResponseMessage reversePayment(UUID paymentUuid) {
        TeacherPayment payment = paymentRepository.findByUuid(paymentUuid)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found."));
        List<UUID> authorized = userScopeService.getAuthorizedSchoolUuids();
        if (authorized != null && (payment.getSchool() == null
                || !authorized.contains(payment.getSchool().getUuid()))) {
            throw new EntityNotFoundException("Payment not found.");
        }
        if (Boolean.TRUE.equals(payment.getDeleted())) {
            return new ResponseMessage("Payment already reversed.");
        }

        payment.setDeleted(Boolean.TRUE);
        paymentRepository.save(payment);
        expenseService.reverse(payment.getExpense());

        // The allocations stay on the row for audit; every query that reads them filters on
        // payment.deleted, so the payslips they pointed at are owed money again from this moment.
        for (TeacherPaymentAllocation allocation : allocationRepository.findAllByPayment_Uuid(paymentUuid)) {
            TeacherPayslip payslip = allocation.getPayslip();
            if (payslip == null) continue;
            applyStatus(payslip, allocationRepository.paidAmountOf(payslip.getUuid()));
            payslipRepository.save(payslip);
        }

        writeEntry(payment.getTeacher(), PayrollEnums.BalanceEntryType.PAYMENT, nz(payment.getAmount()),
                null, payment, "Payment reversed", payment.getNote(), userService.getCurrentUser());

        return new ResponseMessage("Payment reversed; the amount is back on the balance.");
    }

    // ---- Called by the payslip lifecycle ---------------------------------------------------------

    /**
     * Credit an approved payslip to the teacher's balance. Doing nothing twice is the point: a payslip
     * that was approved, reopened and approved again must end up credited once, not twice.
     *
     * <p>A payslip whose deductions swallowed its earnings has nothing to credit and nothing to pay,
     * so it is settled outright rather than sitting on the balance as a month nobody can close.
     */
    @Transactional
    public void accrueOnApproval(TeacherPayslip payslip) {
        long netPay = nz(payslip.getNetPay());
        if (netPay <= 0) {
            payslip.setStatus(PayrollEnums.PayslipStatus.PAID);
            return;
        }
        if (outstandingAccrualOf(payslip) > 0) return;

        writeEntry(payslip.getTeacher(), PayrollEnums.BalanceEntryType.ACCRUAL, netPay,
                payslip, null, "Payslip approved — " + periodOf(payslip), null, null);
    }

    /**
     * Take an approval back. Only legal while nothing has been paid against the month — money that has
     * already changed hands cannot be un-owed by editing the payslip it came from.
     */
    @Transactional
    public void reverseAccrual(TeacherPayslip payslip) {
        long paid = allocationRepository.paidAmountOf(payslip.getUuid());
        if (paid > 0) {
            throw new ValidationException(String.format(
                    "%d has already been paid against this payslip; reverse the payment before reopening it.",
                    paid));
        }
        long outstanding = outstandingAccrualOf(payslip);
        if (outstanding == 0) return;

        writeEntry(payslip.getTeacher(), PayrollEnums.BalanceEntryType.REVERSAL, -outstanding,
                payslip, null, "Approval withdrawn — " + periodOf(payslip), null,
                userService.getCurrentUser());
    }

    /**
     * What the school still owes this teacher in total. Read straight off the ledger, so it includes
     * every month left open rather than just the one being looked at.
     */
    @Transactional(readOnly = true)
    public long balanceOf(UUID teacherUuid) {
        return balanceEntryRepository.balanceOf(teacherUuid);
    }

    /** What has been paid against one payslip, for the detail panel. */
    @Transactional(readOnly = true)
    public long paidAmountOf(UUID payslipUuid) {
        return allocationRepository.paidAmountOf(payslipUuid);
    }

    /** The same figure for a page of payslips, so the list does not run a query per row. */
    @Transactional(readOnly = true)
    public Map<UUID, Long> paidAmountsOf(Collection<UUID> payslipUuids) {
        if (payslipUuids == null || payslipUuids.isEmpty()) return Map.of();
        return toLongMap(allocationRepository.paidAmountsOf(payslipUuids));
    }

    // ---- internals -------------------------------------------------------------------------------

    /**
     * Write the payment, spread it over the payslips in the order given, and book the cash-flow row.
     * {@code candidates} is already in the order the money should be applied in.
     */
    private ResTeacherPayment record(Teacher teacher, List<TeacherPayslip> candidates, long amount,
                                     ReqTeacherPayment req, long balanceBefore) {
        User currentUser = userService.getCurrentUser();
        LocalDate date = req != null && req.getPaymentDate() != null
                ? req.getPaymentDate() : LocalDate.now(UZ_ZONE);
        PayrollEnums.TeacherPaymentType type = req != null && req.getType() != null
                ? req.getType() : PayrollEnums.TeacherPaymentType.ADVANCE;
        String teacherName = TeacherPayslipService.userName(teacher.getUser());

        TeacherPayment payment = new TeacherPayment();
        payment.setTeacher(teacher);
        payment.setSchool(teacher.getSchool());
        payment.setType(type);
        payment.setAmount(amount);
        payment.setMethod(req != null ? req.getMethod() : null);
        payment.setPaymentDate(date);
        payment.setNote(req != null ? req.getNote() : null);
        payment.setCreatedBy(currentUser);
        payment = paymentRepository.save(payment);

        List<TeacherPaymentAllocation> allocations = allocate(payment, candidates, amount);
        allocationRepository.saveAll(allocations);

        SchoolExpense expense = expenseService.recordTeacherPayment(
                teacher.getSchool(),
                (type == PayrollEnums.TeacherPaymentType.ADVANCE ? "Advance: " : "Salary: ") + teacherName,
                amount, payment.getMethod(), date, payment.getNote(), currentUser);
        payment.setExpense(expense);
        payment = paymentRepository.save(payment);

        writeEntry(teacher, PayrollEnums.BalanceEntryType.PAYMENT, -amount, null, payment,
                (type == PayrollEnums.TeacherPaymentType.ADVANCE ? "Advance paid" : "Salary paid"),
                payment.getNote(), currentUser);

        return toPaymentResponse(payment, balanceBefore - amount);
    }

    /**
     * Spread one amount across the months it settles, filling each in turn. The caller has already
     * checked the amount fits, so every som finds a home.
     */
    private List<TeacherPaymentAllocation> allocate(TeacherPayment payment,
                                                    List<TeacherPayslip> candidates,
                                                    long amount) {
        List<TeacherPaymentAllocation> allocations = new ArrayList<>();
        long left = amount;

        for (TeacherPayslip payslip : candidates) {
            if (left <= 0) break;
            long alreadyPaid = allocationRepository.paidAmountOf(payslip.getUuid());
            long owed = nz(payslip.getNetPay()) - alreadyPaid;
            if (owed <= 0) continue;

            long take = Math.min(owed, left);
            TeacherPaymentAllocation allocation = new TeacherPaymentAllocation();
            allocation.setPayment(payment);
            allocation.setPayslip(payslip);
            allocation.setAmount(take);
            allocations.add(allocation);

            applyStatus(payslip, alreadyPaid + take);
            payslipRepository.save(payslip);
            left -= take;
        }

        if (left > 0) {
            // Only reachable if the payslips moved between the balance check and here, which the row
            // lock rules out. Failing loudly beats writing a payment that does not add up.
            throw new ValidationException(
                    "Could not allocate the full amount; the teacher's open payslips changed. Try again.");
        }
        return allocations;
    }

    /** Where a payslip stands once {@code paid} has been applied to it. */
    private void applyStatus(TeacherPayslip payslip, long paid) {
        long netPay = nz(payslip.getNetPay());
        if (paid >= netPay) {
            payslip.setStatus(PayrollEnums.PayslipStatus.PAID);
            if (payslip.getPaidAt() == null) {
                payslip.setPaidAt(Instant.now());
                payslip.setPaidBy(userService.getCurrentUser());
                payslip.setPaymentDate(LocalDate.now(UZ_ZONE));
            }
        } else if (paid > 0) {
            payslip.setStatus(PayrollEnums.PayslipStatus.PARTIALLY_PAID);
        } else {
            payslip.setStatus(PayrollEnums.PayslipStatus.APPROVED);
            payslip.setPaidAt(null);
            payslip.setPaidBy(null);
            payslip.setPaymentDate(null);
        }
    }

    private void writeEntry(Teacher teacher, PayrollEnums.BalanceEntryType type, long amount,
                            TeacherPayslip payslip, TeacherPayment payment,
                            String title, String note, User createdBy) {
        TeacherBalanceEntry entry = new TeacherBalanceEntry();
        entry.setTeacher(teacher);
        entry.setSchool(teacher != null ? teacher.getSchool() : null);
        entry.setEntryType(type);
        entry.setAmount(amount);
        entry.setPayslip(payslip);
        entry.setPayment(payment);
        entry.setTitle(title);
        entry.setNote(note);
        entry.setOccurredAt(Instant.now());
        entry.setCreatedBy(createdBy);
        balanceEntryRepository.save(entry);
    }

    /** How much of this payslip is still standing on the ledger, accruals net of reversals. */
    private long outstandingAccrualOf(TeacherPayslip payslip) {
        return balanceEntryRepository.findAllByPayslip_UuidAndEntryTypeIn(
                        payslip.getUuid(),
                        List.of(PayrollEnums.BalanceEntryType.ACCRUAL,
                                PayrollEnums.BalanceEntryType.REVERSAL))
                .stream()
                .mapToLong(entry -> nz(entry.getAmount()))
                .sum();
    }

    private long outstandingOf(List<TeacherPayslip> payslips) {
        long total = 0L;
        for (TeacherPayslip payslip : payslips) {
            long remaining = nz(payslip.getNetPay()) - allocationRepository.paidAmountOf(payslip.getUuid());
            if (remaining > 0) total += remaining;
        }
        return total;
    }

    private static long resolveAmount(ReqTeacherPayment req, long ceiling) {
        if (req != null && Boolean.TRUE.equals(req.getPayFullBalance())) {
            if (ceiling <= 0) throw new ValidationException("There is nothing outstanding to pay.");
            return ceiling;
        }
        if (req == null || req.getAmount() == null || req.getAmount() <= 0) {
            throw new ValidationException("amount is required and must be positive.");
        }
        return req.getAmount();
    }

    private Teacher loadTeacherInScope(UUID teacherUuid) {
        Teacher teacher = teacherRepository.findByUser_Uuid(teacherUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.TEACHER_NOT_FOUND.getKey()));
        List<UUID> authorized = userScopeService.getAuthorizedSchoolUuids();
        if (authorized != null && (teacher.getSchool() == null
                || !authorized.contains(teacher.getSchool().getUuid()))) {
            throw new EntityNotFoundException(MessageKey.TEACHER_NOT_FOUND.getKey());
        }
        return teacher;
    }

    private ResTeacherPayment toPaymentResponse(TeacherPayment payment, Long balanceAfter) {
        List<ResTeacherPayment.Allocation> allocations = new ArrayList<>();
        for (TeacherPaymentAllocation allocation : allocationRepository.findAllByPayment_Uuid(payment.getUuid())) {
            TeacherPayslip payslip = allocation.getPayslip();
            allocations.add(ResTeacherPayment.Allocation.builder()
                    .payslipUuid(payslip != null ? payslip.getUuid() : null)
                    .period(payslip != null ? periodOf(payslip) : null)
                    .amount(nz(allocation.getAmount()))
                    .payslipStatus(payslip != null ? payslip.getStatus() : null)
                    .build());
        }

        Teacher teacher = payment.getTeacher();
        String name = teacher != null ? TeacherPayslipService.userName(teacher.getUser()) : null;
        return ResTeacherPayment.builder()
                .uuid(payment.getUuid())
                .teacherUuid(teacher != null && teacher.getUser() != null ? teacher.getUser().getUuid() : null)
                .teacherName(name)
                .type(payment.getType())
                .amount(nz(payment.getAmount()))
                .method(payment.getMethod())
                .paymentDate(payment.getPaymentDate())
                .note(payment.getNote())
                .balanceAfter(balanceAfter)
                .allocations(allocations)
                .createdByName(TeacherPayslipService.userName(payment.getCreatedBy()))
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private static ResBalanceEntry toEntryResponse(TeacherBalanceEntry entry) {
        TeacherPayslip payslip = entry.getPayslip();
        return ResBalanceEntry.builder()
                .uuid(entry.getUuid())
                .entryType(entry.getEntryType())
                .amount(nz(entry.getAmount()))
                .title(entry.getTitle())
                .note(entry.getNote())
                .payslipUuid(payslip != null ? payslip.getUuid() : null)
                .period(payslip != null ? periodOf(payslip) : null)
                .paymentUuid(entry.getPayment() != null ? entry.getPayment().getUuid() : null)
                .occurredAt(entry.getOccurredAt())
                .createdByName(TeacherPayslipService.userName(entry.getCreatedBy()))
                .build();
    }

    private static String periodOf(TeacherPayslip payslip) {
        return payslip.getPeriodYear() != null && payslip.getPeriodMonth() != null
                ? YearMonth.of(payslip.getPeriodYear(), payslip.getPeriodMonth()).toString()
                : null;
    }

    private static String joinName(String firstName, String lastName) {
        return ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
    }

    private static Map<UUID, Long> toLongMap(List<Object[]> rows) {
        Map<UUID, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }

    private static long nz(Long v) {
        return v != null ? v : 0L;
    }

    private static String blankToNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }
}
