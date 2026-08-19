package uz.tune.mentourBiz.rest.service.student;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.StudentDiscount;
import uz.tune.mentourBiz.rest.enums.DiscountType;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.payload.req.student.ReqStudentDiscount;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.student.ResStudentDiscount;
import uz.tune.mentourBiz.rest.repository.student.StudentDiscountRepository;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.rest.service.user.UserService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Grants and resolves per-student discounts.
 *
 * <p>A student has at most one discount of each {@link DiscountType} in force at a time — a fixed som
 * amount, a percentage, or one of each, which then both come off the same fee. Expired and switched-off
 * rows stay on file as history. The billing side only ever calls {@link #discountFor(Student, long)},
 * which returns the som to knock off a charge (0 when the student has no discount), so a school that
 * never grants one is unaffected.
 */
@Service
@RequiredArgsConstructor
public class StudentDiscountService {

    private static final ZoneId UZ_ZONE = ZoneId.of("Asia/Tashkent");

    private final StudentDiscountRepository discountRepository;
    private final StudentRepo studentRepo;
    private final AuthToViewEntity authToViewEntity;
    private final UserService userService;

    // ---- Billing hooks ---------------------------------------------------------------------------

    /**
     * The discounts in force for the student today: at most one per type, so 0, 1 or 2 rows.
     */
    @Transactional(readOnly = true)
    public List<StudentDiscount> findActiveDiscounts(Student student) {
        if (student == null || student.getUuid() == null) return List.of();
        LocalDate today = LocalDate.now(UZ_ZONE);
        return onePerType(discountRepository.findActiveForStudent(student.getUuid(), today), today);
    }

    /**
     * How much to take off a {@code price} for this student right now — every discount they hold,
     * combined. Returns 0 when the student has none, and never more than the price itself.
     */
    @Transactional(readOnly = true)
    public long discountFor(Student student, long price) {
        if (price <= 0) return 0L;
        return StudentDiscount.totalDiscountOn(findActiveDiscounts(student), price);
    }

    /**
     * The discounts in force today for each of the given students, keyed by student uuid. One query for
     * the whole set, so a finance list can show discounts without a lookup per row. Students without a
     * discount are simply absent from the map.
     */
    @Transactional(readOnly = true)
    public Map<UUID, List<StudentDiscount>> activeDiscountsFor(Collection<UUID> studentUuids) {
        if (studentUuids == null || studentUuids.isEmpty()) return Map.of();
        LocalDate today = LocalDate.now(UZ_ZONE);

        Map<UUID, List<StudentDiscount>> byStudent = new HashMap<>();
        for (StudentDiscount discount : discountRepository.findActiveForStudents(studentUuids, today)) {
            if (discount.getStudent() == null || !discount.appliesOn(today)) continue;
            byStudent.computeIfAbsent(discount.getStudent().getUuid(), k -> new ArrayList<>()).add(discount);
        }
        byStudent.replaceAll((studentUuid, discounts) -> onePerType(discounts, today));
        return byStudent;
    }

    /**
     * Keeps one discount per type out of {@code candidates}. Creating a second discount of a type the
     * student already holds is refused, so this only ever has to break a tie in a data set that
     * predates that rule — rows arrive newest first and the newest wins.
     */
    private static List<StudentDiscount> onePerType(List<StudentDiscount> candidates, LocalDate today) {
        Map<DiscountType, StudentDiscount> byType = new EnumMap<>(DiscountType.class);
        for (StudentDiscount discount : candidates) {
            if (discount.getType() == null || !discount.appliesOn(today)) continue;
            byType.putIfAbsent(discount.getType(), discount);
        }
        return List.copyOf(byType.values());
    }

    // ---- CRUD ------------------------------------------------------------------------------------

    /**
     * Grant a discount. A student may hold one of each type, so this is refused only when a discount of
     * the <em>same</em> type is already in force — change or delete that one instead, rather than
     * stacking two fixed amounts on the same fee by accident. A FIXED discount alongside an existing
     * PERCENT one (or the other way round) is fine and both apply.
     */
    @Transactional
    public ResStudentDiscount create(ReqStudentDiscount req) {
        if (req == null || req.getStudentUuid() == null) {
            throw new ValidationException("studentUuid is required.");
        }
        if (req.getType() == null) {
            throw new ValidationException("type is required (FIXED or PERCENT).");
        }
        Student student = loadStudentInScope(req.getStudentUuid());
        LocalDate today = LocalDate.now(UZ_ZONE);
        requireTypeFree(student.getUuid(), req.getType(), null, today);

        StudentDiscount discount = new StudentDiscount();
        discount.setStudent(student);
        discount.setCreatedBy(userService.getCurrentUser());
        apply(discount, req, true);
        return new ResStudentDiscount(discountRepository.save(discount), today);
    }

    /**
     * Change an existing discount. Omitted fields keep their stored value. Switching it to a type the
     * student already holds is refused for the same reason creating a duplicate is.
     */
    @Transactional
    public ResStudentDiscount update(UUID discountUuid, ReqStudentDiscount req) {
        StudentDiscount discount = loadDiscountInScope(discountUuid);
        LocalDate today = LocalDate.now(UZ_ZONE);

        if (req != null && req.getType() != null && req.getType() != discount.getType()
                && discount.getStudent() != null) {
            requireTypeFree(discount.getStudent().getUuid(), req.getType(), discount.getUuid(), today);
        }

        apply(discount, req != null ? req : new ReqStudentDiscount(), false);
        return new ResStudentDiscount(discountRepository.save(discount), today);
    }

    /** Refuses when the student already holds a discount of {@code type}, ignoring {@code selfUuid}. */
    private void requireTypeFree(UUID studentUuid, DiscountType type, UUID selfUuid, LocalDate today) {
        boolean taken = discountRepository.findActiveForStudent(studentUuid, today).stream()
                .filter(d -> !d.getUuid().equals(selfUuid))
                .anyMatch(d -> d.getType() == type && d.appliesOn(today));
        if (taken) {
            throw new ValidationException(MessageKey.STUDENT_DISCOUNT_TYPE_EXISTS.getKey());
        }
    }

    /**
     * Remove a discount outright. Charges already made keep the discount they were given — this only
     * stops future ones. Use {@code isActive = false} instead to keep the row on file.
     */
    @Transactional
    public ResponseMessage delete(UUID discountUuid) {
        discountRepository.delete(loadDiscountInScope(discountUuid));
        return new ResponseMessage("Student discount deleted.");
    }

    /** The discounts in force for the student today — one per type, empty when there are none. */
    @Transactional(readOnly = true)
    public List<ResStudentDiscount> getActive(UUID studentUuid) {
        Student student = loadStudentInScope(studentUuid);
        LocalDate today = LocalDate.now(UZ_ZONE);
        return findActiveDiscounts(student).stream()
                .map(d -> new ResStudentDiscount(d, today))
                .toList();
    }

    /** Every discount ever granted to the student, newest first. */
    @Transactional(readOnly = true)
    public List<ResStudentDiscount> listForStudent(UUID studentUuid) {
        Student student = loadStudentInScope(studentUuid);
        LocalDate today = LocalDate.now(UZ_ZONE);
        return discountRepository.findAllByStudent_UuidOrderByCreatedAtDesc(student.getUuid()).stream()
                .map(d -> new ResStudentDiscount(d, today))
                .toList();
    }

    // ---- internals -------------------------------------------------------------------------------

    /**
     * Writes the request onto the row and validates the result. On create everything required must be
     * present; on update only the supplied fields move, but the outcome still has to be a valid
     * discount (e.g. a PERCENT discount always ends up with a percent and no amount).
     */
    private void apply(StudentDiscount discount, ReqStudentDiscount req, boolean creating) {
        if (req.getType() != null) discount.setType(req.getType());
        if (creating && discount.getType() == null) {
            throw new ValidationException("type is required (FIXED or PERCENT).");
        }

        if (req.getAmount() != null) discount.setAmount(req.getAmount());
        if (req.getPercent() != null) discount.setPercent(req.getPercent());
        if (req.getNote() != null) discount.setNote(req.getNote());
        if (req.getIsActive() != null) discount.setIsActive(req.getIsActive());

        if (discount.getType() == DiscountType.FIXED) {
            if (discount.getAmount() == null || discount.getAmount() <= 0) {
                throw new ValidationException("amount must be greater than 0 for a FIXED discount.");
            }
            discount.setPercent(null);
        } else {
            Integer percent = discount.getPercent();
            if (percent == null || percent <= 0 || percent > 100) {
                throw new ValidationException("percent must be between 1 and 100 for a PERCENT discount.");
            }
            discount.setAmount(null);
        }

        // Window. startDate anchors it; durationMonths (or its absence) decides where it ends. Both are
        // recomputed together so an update that moves only one of them cannot leave a stale end date.
        if (req.getStartDate() != null) discount.setStartDate(req.getStartDate());
        if (discount.getStartDate() == null) discount.setStartDate(LocalDate.now(UZ_ZONE));

        Integer months = discount.getDurationMonths();
        if (Boolean.TRUE.equals(req.getPermanent())) {
            months = null;
        } else if (req.getDurationMonths() != null) {
            months = req.getDurationMonths();
        } else if (creating) {
            months = null;  // neither duration nor permanent given: permanent
        }
        if (months != null && months <= 0) {
            throw new ValidationException("durationMonths must be greater than 0, or omit it for a permanent discount.");
        }
        discount.setDurationMonths(months);
        discount.setEndDate(months == null ? null : discount.getStartDate().plusMonths(months));
    }

    private Student loadStudentInScope(UUID studentUuid) {
        if (studentUuid == null) throw new ValidationException("studentUuid is required.");
        Student student = studentRepo.findByUuid(studentUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
        authToViewEntity.authorizeActionUponStudent(student);
        return student;
    }

    private StudentDiscount loadDiscountInScope(UUID discountUuid) {
        StudentDiscount discount = discountRepository.findByUuid(discountUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.STUDENT_DISCOUNT_NOT_FOUND.getKey()));
        authToViewEntity.authorizeActionUponStudent(discount.getStudent());
        return discount;
    }
}
