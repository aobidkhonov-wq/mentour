package uz.tune.mentourBiz.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.config.logger.Logger;
import uz.tune.mentourBiz.rest.domain.Message;
import uz.tune.mentourBiz.rest.domain.Notification;
import uz.tune.mentourBiz.rest.domain.PaymentPackage;
import uz.tune.mentourBiz.rest.domain.SchoolAcademicConfig;
import uz.tune.mentourBiz.rest.domain.StudentParentContact;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.postExercise.UnitProgress;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.Course;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.CourseLesson;
import uz.tune.mentourBiz.rest.domain.schoolManagement.schedule.AttendanceRecord;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.BillingPlan;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.endpoint.FinanceEndpoint;
import uz.tune.mentourBiz.rest.enums.*;
import uz.tune.mentourBiz.rest.repository.*;
import uz.tune.mentourBiz.rest.repository.user.UserRepo;
import uz.tune.mentourBiz.rest.repository.course.CourseLessonRepo;
import uz.tune.mentourBiz.rest.repository.course.CourseRepo;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.schedule.AttendanceRecordRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.UnitProgressRepository;
import uz.tune.mentourBiz.rest.repository.user.TeacherRepository;
import uz.tune.mentourBiz.rest.service.FinanceService;
import uz.tune.mentourBiz.rest.service.group.enrollment.EnrollmentBillingService;
import uz.tune.mentourBiz.rest.service.TelegramBotService;
import uz.tune.mentourBiz.utils.FcmPushService;
import uz.tune.mentourBiz.whatsApp.WhatsAppService;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.*;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AttendanceNotificationScheduler {

    private final AttendanceRecordRepository attendanceRepository;
    private final StudentParentContactRepository parentContactRepository;
    private final UnitProgressRepository unitProgressRepository;
    private final TelegramBotService telegramBotService;
    private final MessageRepo messageRepo;
    private final PaymentPackageRepo paymentPackageRepo;
    private final EnrollmentRepository enrollmentRepository;
    private final FinanceService financeService;
    private final CourseRepo courseRepo;
    private static final ZoneId UZ_ZONE = ZoneId.of("Asia/Tashkent");

    // How many lapsed billing periods one renewal run will settle for a single enrollment. Two years
    // of arrears is already far past anything legitimate; beyond that the data needs a human, not a
    // pile of automated charges.
    private static final int MAX_RENEWAL_CATCH_UP_PERIODS = 24;
    private final CourseLessonRepo courseLessonRepo;
    private final FinanceEndpoint financeEndpoint;
    private final FinanceTransactionRepo financeTransactionRepo;
    private final TeacherRepository teacherRepository;
    private final SchoolAcademicConfigRepo schoolAcademicConfigRepo;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final WhatsAppService whatsAppService;
    private final FcmPushService fcmPushService;
    private final FcmTokenRepo fcmTokenRepo;
    private final UserRepo userRepo;
    private final NotificationRepo notificationRepo;
    private final ObjectMapper objectMapper;
    private final EnrollmentBillingService enrollmentBillingService;


    @Scheduled(cron = "${scheduler.attendance.cron:0 0 * * * *}")
    public void processImmediateAttendance() {
        Instant now = Instant.now();
        List<AttendanceRecord> pending = attendanceRepository.findPendingAttendanceNotifications(now);
        Logger.logInfo("[SCHEDULER] fired, pending=" + pending.size());
        for (AttendanceRecord record : pending) {
            try {
                if (Boolean.TRUE.equals(record.getIsMarked()) && Boolean.FALSE.equals(record.getIsAttendanceNotified())) {
                    sendImmediateAttendanceUpdate(record);
                }
            } catch (Exception e) {
                System.err.println("Error in immediate notification: " + e.getMessage());
            }
        }
    }

    @Scheduled(cron = "0 0 0 1 * *", zone = "Asia/Tashkent")
    @Transactional
    public void resetMonthlyTeacherAllowances() {
        teacherRepository.resetAllTeacherAllowances();
    }

    @Scheduled(cron = "0 0 20 * * SUN")
    @Transactional
    public void processWeeklyReports() {
        LocalDate today = LocalDate.now(UZ_ZONE);
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate saturday = today.with(DayOfWeek.SATURDAY);

        Instant startOfWeek = monday.atStartOfDay(UZ_ZONE).toInstant();
        Instant endOfWeek = saturday.atTime(LocalTime.MAX).atZone(UZ_ZONE).toInstant();

        List<Enrollment> activeEnrollments = enrollmentRepository.findAllByGroup_GroupStatusAndStatus(
                GroupStatus.ACTIVE, EnrollmentStatus.ONGOING);

        for (Enrollment enrollment : activeEnrollments) {
            try {
                sendWeeklySummary(enrollment, startOfWeek, endOfWeek);
            } catch (Exception e) {
                System.err.println("Error processing weekly report: " + e.getMessage());
            }
        }
    }

    private void sendImmediateAttendanceUpdate(AttendanceRecord record) {
        Student student = record.getStudent();
        CourseLesson lesson = record.getLesson();
        List<StudentParentContact> parents = parentContactRepository.findAllByStudentAndIsActiveTrue(student);

        if (parents.isEmpty()) {
            record.setIsAttendanceNotified(true);
            attendanceRecordRepository.saveAndFlush(record);
            return;
        }

        String studentFullName = student.getUser().getFirstName() + " " + student.getUser().getLastName();

        Integer utcOffset = null;
        try {
            utcOffset = lesson.getCourse().getGroup().getBranch().getSchool().getUtcOffset();
        } catch (Exception ignored) {}
        ZoneId schoolZone = (utcOffset != null)
                ? ZoneOffset.ofHours(utcOffset)
                : UZ_ZONE;

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(schoolZone);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(schoolZone);
        String lessonDate = dateFormatter.format(lesson.getStartTime());
        String lessonTime = timeFormatter.format(lesson.getStartTime()) + " – " + timeFormatter.format(lesson.getEndTime());

        String subjectName = "";
        if (lesson.getCourse() != null) {
            var group = lesson.getCourse().getGroup();
            if (group != null && group.getLevel() != null && group.getLevel().getSubject() != null) {
                subjectName = group.getLevel().getSubject().getName();
            }
            if (subjectName.isBlank()) {
                subjectName = lesson.getCourse().getName();
            }
        }

        String fcmTitle = studentFullName + (subjectName.isBlank() ? "" : " – " + subjectName);
        for (StudentParentContact parent : parents) {
            sendFcmToParentUser(parent, lang -> {
                String statusLabel = getStatusTranslation(record.getStatus(), lang);
                return new String[]{fcmTitle, statusLabel + " · " + lessonDate + " " + lessonTime};
            });
        }

        record.setIsAttendanceNotified(true);
        attendanceRecordRepository.saveAndFlush(record);
    }

    private void sendWeeklySummary(Enrollment enrollment, Instant start, Instant end) {
        Student student = enrollment.getStudent();
        List<StudentParentContact> parents = parentContactRepository.findAllByStudentAndIsActiveTrue(student);
        if (parents.isEmpty()) return;

        Course course = courseRepo.findAllByGroupAndStatus(enrollment.getGroup(), CourseStatus.ACTIVE).stream().findFirst().orElse(null);
        if (course == null) return;

        List<CourseLesson> weekLessons = courseLessonRepo.findWithFilters(
                null, List.of(LessonStatus.STUDENT_APP, LessonStatus.FINISHED), null, null,
                List.of(course.getUuid()), start, end, org.springframework.data.domain.Pageable.unpaged()
        ).getContent();

        if (weekLessons.isEmpty()) return;

        List<CourseLesson> sortedLessons = weekLessons.stream()
                .sorted(Comparator.comparing(CourseLesson::getStartTime))
                .collect(Collectors.toList());
        if (sortedLessons.size() > 1) { sortedLessons.remove(sortedLessons.size() - 1); } else { return; }

        // Attendance count
        List<AttendanceRecord> weekRecords = attendanceRecordRepository.findAllByStudentAndLessonIn(student, sortedLessons);
        int totalLessons = sortedLessons.size();
        final int attended = (int) weekRecords.stream()
                .filter(ar -> ar.getStatus() == AttendanceStatus.PRESENT || ar.getStatus() == AttendanceStatus.LATE)
                .count();

        // Average grade (unit progress)
        int totalScore = 0;
        int unitCount = 0;
        for (CourseLesson lesson : sortedLessons) {
            for (Unit unit : lesson.getUnits()) {
                totalScore += unitProgressRepository.findByStudentAndUnit(student, unit)
                        .map(UnitProgress::getProgressPercentage).orElse(0);
                unitCount++;
            }
        }
        final int avgGrade = unitCount > 0 ? (totalScore / unitCount) : 0;
        final String studentFirstName = student.getUser().getFirstName();

        for (StudentParentContact parent : parents) {
            sendFcmToParentUser(parent, lang -> {
                String title = switch (lang) {
                    case UZB -> "Haftalik hisobot – " + studentFirstName;
                    case KAA -> "Ҳәптелик есеп – " + studentFirstName;
                    case TJK -> "Ҳисоботи ҳафтагӣ – " + studentFirstName;
                    case KRG -> "Жумалык отчёт – " + studentFirstName;
                    default  -> "Еженедельный отчёт – " + studentFirstName;
                };
                String lessonWord = switch (lang) {
                    case UZB -> "dars";
                    case KAA -> "сабақ";
                    case TJK -> "дарс";
                    case KRG -> "сабак";
                    default  -> "урок";
                };
                String gradeWord = switch (lang) {
                    case UZB -> "o'rtacha ball";
                    case KAA -> "орта балл";
                    case TJK -> "миёна балл";
                    case KRG -> "орточо балл";
                    default  -> "средний балл";
                };
                return new String[]{title, attended + "/" + totalLessons + " " + lessonWord + " · " + avgGrade + "% " + gradeWord};
            });
        }
    }



//    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void processNotifications() {
        Instant now = Instant.now();
        List<AttendanceRecord> pendingRecords = attendanceRepository.findPendingNotifications(now);

        for (AttendanceRecord record : pendingRecords) {
            try {
                processSingleRecord(record);
            } catch (Exception e) {
                System.err.println("Error processing record: " + e.getMessage());
            }
        }
        attendanceRepository.saveAll(pendingRecords);
    }

    /**
     * @deprecated Old PaymentPackage monthly charger. DISABLED (no {@code @Scheduled}, no manual
     * endpoint) to stop double-charging: FIXED_MONTHLY billing is now handled by
     * {@link #processMonthlyRenewals()}. Kept temporarily; will be removed with the PaymentPackage code.
     */
    @Deprecated
    @Transactional
    public void processMonthlyCharges() {
        LocalDate today = LocalDate.now(UZ_ZONE);
        int dayOfMonth = today.getDayOfMonth();

        // Get all packages due today
        List<PaymentPackage> packagesDueToday = paymentPackageRepo.findAllByPaymentDueDateAndSchoolStatus(dayOfMonth, SchoolStatus.ACTIVE);

        for (PaymentPackage pkg : packagesDueToday) {
            School school = pkg.getSchool();

            if (school.getOrganization() != null) {
                if (school.getOrganization().getStatus() == SchoolStatus.FROZEN) {
                    Logger.logInfo("Skipping automated charges for " + school.getName() + " - Organization is Frozen.");
                    continue;
                }
            }

            if (!school.isPaymentActive()) continue;

            List<Course> courses = courseRepo.findAllByPaymentPackage_UuidAndStatus(pkg.getUuid(), CourseStatus.ACTIVE);

            for (Course course : courses) {
                List<Enrollment> enrollments = enrollmentRepository.findAllByGroup_UuidAndStatusAndStudent_User_Status(
                        course.getGroup().getUuid(), EnrollmentStatus.ONGOING, UserStatus.ACTIVE);

                for (Enrollment e : enrollments) {
                    Student student = e.getStudent();
                    if (!student.isPaymentActivated()) continue;

                    financeService.applyMonthlyCharge(student, pkg, "Automated monthly charge for: " + course.getName());
                    notifyParentsOfCharge(student, pkg);
                }
            }
        }
    }

    /**
     * MONTHLY billing plans: when a paid period lapses, auto-charge the next period against the
     * student's som balance ({@code currentBalance}). The charge always goes through; if the balance
     * is insufficient the student simply ends up in debt (overdue) rather than being frozen.
     *
     * <p>An enrollment several periods in arrears is settled in full in a single run. Charging one
     * period per daily run used to dribble the backlog out over as many days as it had periods, so a
     * student months behind collected several separate monthly charges on consecutive days — which in
     * turn scattered one month's revenue across the wrong payroll months.
     */
    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Tashkent")
    @Transactional
    public void processMonthlyRenewals() {
        Instant now = Instant.now();
        List<Enrollment> due = enrollmentRepository.findAllByStatusAndBillingTypeAndPaidUntilBefore(
                EnrollmentStatus.ONGOING, BillingPlanType.FIXED_MONTHLY, now);

        for (Enrollment enrollment : due) {
            try {
                BillingPlan plan = enrollment.getBillingPlan();
                if (plan == null) continue;

                Student student = enrollment.getStudent();
                String note = "Monthly renewal: " + enrollment.getGroup().getName() + " (" + plan.getName() + ")";
                int months = plan.getCountMonth() != null && plan.getCountMonth() > 0 ? plan.getCountMonth() : 1;

                long charged = 0L;
                int periods = 0;
                Instant paidUntil = enrollment.getPaidUntil() != null ? enrollment.getPaidUntil() : now;

                // The enrollment is in this list precisely because a period has lapsed, so the first
                // charge is unconditional; the loop then keeps going while it is still behind. The cap
                // stops a wildly stale paidUntil from billing a student dozens of times in one go.
                do {
                    charged += enrollmentBillingService.chargeBillingPlan(
                            student, plan, enrollment.getGroup(), null, note);
                    // Always extend from the previous period end, so the billing day stays anchored to
                    // the enrollment's start date instead of drifting to whatever day the charge ran on.
                    paidUntil = paidUntil.atZone(UZ_ZONE).plusMonths(months).toInstant();
                    periods++;
                } while (paidUntil.isBefore(now) && periods < MAX_RENEWAL_CATCH_UP_PERIODS);

                if (paidUntil.isBefore(now)) {
                    Logger.logInfo("Monthly renewal for enrollment " + enrollment.getId() + " capped at "
                            + MAX_RENEWAL_CATCH_UP_PERIODS + " period(s); still in arrears.");
                }

                enrollment.setPaidUntil(paidUntil);
                enrollment.setLastPaidAt(now);
                long already = enrollment.getAmountPaid() != null ? enrollment.getAmountPaid() : 0L;
                // What the student was actually billed: the plan price less their discount, if any.
                enrollment.setAmountPaid(already + charged);

                // Renew the lesson cap (if any) for every period charged, so cumulative consumption
                // keeps working after a catch-up.
                if (plan.getLessonCount() != null) {
                    long currentCap = enrollment.getLessonsTotal() != null ? enrollment.getLessonsTotal() : 0L;
                    enrollment.setLessonsTotal((int) (currentCap + (long) plan.getLessonCount() * periods));
                }
                enrollmentRepository.save(enrollment);
            } catch (Exception e) {
                System.err.println("Error processing monthly renewal: " + e.getMessage());
            }
        }
    }

    private void processSingleRecord(AttendanceRecord record) {
        Student student = record.getStudent();
        CourseLesson lesson = record.getLesson();
        List<StudentParentContact> parents = parentContactRepository.findAllByStudentAndIsActiveTrue(student);

        if (parents.isEmpty()) {
            return;
        }

        for (StudentParentContact p : parents) {
            if (p.isUseTelegram() && p.getTelegramChatId() == null) return;
            if (p.isUseWhatsapp() && (p.getPhoneNumber() == null || p.getPhoneNumber().isBlank())) return;
        }

        String lessonDate = DateTimeFormatter.ofPattern("E dd MMMM yyyy").withZone(UZ_ZONE).format(lesson.getStartTime());
        String studentName = student.getUser().getFirstName();
        String lessonName = lesson.getName();

        for (StudentParentContact parent : parents) {
            Lang lang = parent.getLanguage() != null ? parent.getLanguage() : Lang.UZB;

            // --- 1. UNIT/PROGRESS NOTIFICATION ---
            if (Boolean.FALSE.equals(record.getIsUnitNotified())) {
                List<Unit> units = lesson.getUnits();
                if (!units.isEmpty()) {
                    // TELEGRAM (Detailed List)
                    if (parent.isUseTelegram()) {
                        StringBuilder sb = new StringBuilder();
                        String header = getTemplate("unit_report_header", lang, "📊 <b>Progress Report: {0}</b>\n");
                        sb.append(java.text.MessageFormat.format(header.replace("'", "''"), studentName));
                        for (Unit u : units) {
                            int score = unitProgressRepository.findByStudentAndUnit(student, u).map(UnitProgress::getProgressPercentage).orElse(0);
                            sb.append(java.text.MessageFormat.format("🔹 {0}: {1}%\n", u.getTitle(), score));
                        }
                        telegramBotService.sendMsg(parent.getTelegramChatId(), sb.toString());
                    }
                    // WHATSAPP (Summary Template)
                    if (parent.isUseWhatsapp()) {
                        // Meta Template: unit_report_summary | {{1}}=Name, {{2}}=Lesson, {{3}}=Date
                        whatsAppService.sendTemplateMessage(parent.getPhoneNumber(), "unit_report_summary", lang,
                                List.of(studentName, lessonName, lessonDate));
                    }
                }
            }

            // --- 2. ATTENDANCE NOTIFICATION ---
            if (Boolean.FALSE.equals(record.getIsAttendanceNotified()) && Boolean.TRUE.equals(record.getIsMarked())) {
                String statusTranslation = getStatusTranslation(record.getStatus(), lang);

                // TELEGRAM
                if (parent.isUseTelegram()) {
                    String template = getTemplate("attendance_report_template", lang, "📍 {0} | {1} | {2} | {3}");
                    String msg = java.text.MessageFormat.format(template.replace("'", "''"), studentName, lessonName, lessonDate, statusTranslation);
                    telegramBotService.sendMsg(parent.getTelegramChatId(), msg);
                }
                // WHATSAPP
                if (parent.isUseWhatsapp()) {
                    // Meta Template: attendance_report_template | {{1}}=Name, {{2}}=Lesson, {{3}}=Date, {{4}}=Status
                    whatsAppService.sendTemplateMessage(parent.getPhoneNumber(), "attendance_report_template", lang,
                            List.of(studentName, lessonName, lessonDate, statusTranslation));
                }
            }
        }

        // Only reach here if all parents were processed. Update flags to stop the loop.
        record.setIsUnitNotified(true);
        if (Boolean.TRUE.equals(record.getIsMarked())) {
            record.setIsAttendanceNotified(true);
        }
        attendanceRecordRepository.saveAndFlush(record);
    }

    private void notifyParentsOfCharge(Student student, PaymentPackage pkg) {
        List<StudentParentContact> parents = parentContactRepository.findAllByStudentAndIsActiveTrue(student);
        if (parents.isEmpty()) return;

        String studentName = student.getUser().getFirstName();
        String pkgName = pkg.getName();
        String price = pkg.getPrice() + " " + pkg.getCurrency();
        String balance = String.valueOf(student.getCurrentBalance());

        for (StudentParentContact parent : parents) {
            sendFcmToParentUser(parent, lang -> new String[]{
                    studentName,
                    pkgName + " · " + price + " · Balance: " + balance
            });
        }
    }


    private void sendFcmToParentUser(StudentParentContact contact, Function<Lang, String[]> contentByLang) {
        if (contact.getPhoneNumber() == null || contact.getPhoneNumber().isBlank()) return;
        userRepo.findByUsernameAndStatus(contact.getPhoneNumber(), UserStatus.ACTIVE)
                .filter(u -> u.getRole() == UserRole.PARENT)
                .ifPresentOrElse(parentUser -> {
                    List<uz.tune.mentourBiz.rest.domain.FcmToken> tokens =
                            fcmTokenRepo.findFcmTokensByUserUuids(List.of(parentUser.getUuid()));

                    // Build all language variants for notification history
                    Map<String, Map<String, String>> locMap = new LinkedHashMap<>();
                    for (Lang l : new Lang[]{Lang.UZB, Lang.RUS, Lang.KAA, Lang.TJK, Lang.KRG}) {
                        String[] c = contentByLang.apply(l);
                        locMap.put(l.name(), Map.of("title", c[0], "content", c[1]));
                    }
                    String locJson = null;
                    try { locJson = objectMapper.writeValueAsString(locMap); } catch (Exception ignored) {}

                    // Use RUS as default title/content fallback
                    String[] defaultContent = contentByLang.apply(Lang.RUS);
                    Notification n = new Notification();
                    n.setTitle(defaultContent[0]);
                    n.setContent(defaultContent[1]);
                    n.setLocalizations(locJson);
                    n.setTargetType(NotificationTargetType.INDIVIDUAL_USER);
                    n.setTargetUuid(parentUser.getUuid());
                    n.setCreatedBy("SYSTEM");
                    notificationRepo.save(n);

                    if (tokens.isEmpty()) {
                        Logger.logInfo("[FCM] parent=" + contact.getPhoneNumber() + " tokens=0 (saved to history)");
                        return;
                    }
                    Map<Lang, List<String>> byLang = tokens.stream()
                            .collect(Collectors.groupingBy(
                                    t -> t.getLanguage() != null ? t.getLanguage() : Lang.RUS,
                                    Collectors.mapping(uz.tune.mentourBiz.rest.domain.FcmToken::getToken, Collectors.toList())
                            ));
                    byLang.forEach((lang, tokenList) -> {
                        String[] content = contentByLang.apply(lang);
                        Logger.logInfo("[FCM] parent=" + contact.getPhoneNumber() + " tokens=" + tokenList.size() + " lang=" + lang + " title=" + content[0]);
                        fcmPushService.sendPush(tokenList, content[0], content[1]);
                    });
                }, () -> Logger.logInfo("[FCM] no PARENT user found for phone=" + contact.getPhoneNumber()));
    }

    private String getStatusTranslation(AttendanceStatus status, Lang lang) {
        String key = "status_" + (status != null ? status.name().toLowerCase() : "not_marked");
        return messageRepo.findTopByKeyAndLang(key, lang)
                .or(() -> messageRepo.findTopByKeyAndLang(key, Lang.RUS))
                .map(Message::getMessage)
                .orElse(status != null ? status.name() : "N/A");
    }

    private String getTemplate(String key, Lang lang, String defaultVal) {
        return messageRepo.findTopByKeyAndLang(key, lang).map(Message::getMessage).orElse(defaultVal);
    }
}