package uz.tune.mentourBiz.rest.enums;

import com.google.gson.internal.LinkedTreeMap;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageKey {

    // 1. AUTHENTICATION & SECURITY
    USER_NOT_FOUND("auth.user.not.found", "User not found."),
    PASSWORD_INCORRECT("auth.password.incorrect", "Incorrect password."),
    OLD_PASSWORD_WRONG("auth.password.old.wrong", "The current password you entered is incorrect."),
    PASSWORD_MISMATCH("auth.password.mismatch", "New password and confirmation do not match."),
    USERNAME_TAKEN("auth.username.taken", "This username is already taken."),
    TOKEN_INVALID("auth.token.invalid", "Invalid or malformed security token."),
    TOKEN_EXPIRED("auth.token.expired", "Your session has expired. Please login again."),
    ACCESS_DENIED("auth.access.denied", "Access denied. You do not have permission for this action."),
    UNAUTHORIZED("auth.unauthorized", "You are not authorized to perform this action."),
    ORG_MISMATCH("auth.org.mismatch", "Access Denied: This record belongs to another organization."),
    SCHOOL_CONTEXT_REQUIRED("auth.school.context.required", "Please select a school branch to continue."),
    DIRECTOR_RESTRICTION("auth.director.restriction", "Only System Administrators can modify Director profiles."),
    USERNAME_TOO_SHORT("auth.username.short", "Username and Password must be at least 5 characters."),
    SHOP_TEACHER_LIMIT("shop.teacher.limit", "Insufficient allowance. Monthly coin limit reached."),

    // 2. SCHOOL & SUBSCRIPTION
    SCHOOL_NOT_FOUND("school.not.found", "School branch not found."),
    SCHOOL_FROZEN("school.frozen", "This school is frozen. Please contact administration."),
    SUBSCRIPTION_EXPIRED("school.subscription.expired", "Access Denied: The school subscription has expired."),
    BILLING_INACTIVE("school.billing.inactive", "Billing module is not activated for this branch."),
    BILLING_ALREADY_ACTIVE("school.billing.already.active", "Billing is already active for this branch."),
    BILLING_PLAN_NOT_FOUND("billing.plan.not.found", "Billing plan not found."),
    BILLING_PLAN_GROUP_MISMATCH("billing.plan.group.mismatch", "Billing plan does not belong to this group."),
    STUDENT_LIMIT_REACHED("school.limit.students", "Subscription Limit reached! Cannot add more students."),
    REGION_NOT_FOUND("school.region.not.found", "Region not found."),
    SCHOOL_BOOKS_REQUIRED("school.books.required", "School Books must be assigned!"),

    // 3. COURSE, GROUPS & LESSONS
    COURSE_NOT_FOUND("course.not.found", "Course not found."),
    GROUP_NO_ACTIVE_COURSE("course.group.no.active.course", "This group has no active courses."),
    COURSE_FINISHED("course.finished", "This course is finished. Submissions are closed."),
    UNIT_DUPLICATE_IN_COURSE("course.unit.duplicate", "This unit is already assigned to another lesson in this course."),
    UNIT_ALREADY_SCHEDULED("course.unit.already.scheduled", "This lesson is already scheduled."),
    GROUP_NOT_FOUND("course.group.not.found", "Group or Class not found."),
    BRANCH_NOT_FOUND("course.branch.not.found", "Branch not found."),
    LEVEL_NOT_FOUND("course.level.not.found", "Level not found."),
    SUBJECT_NOT_FOUND("subject.not.found", "Subject not found."),
    SUBJECT_NAME_TAKEN("subject.name.taken", "A subject with this name already exists."),
    SUBJECT_IN_USE("subject.in.use", "Cannot delete this subject because it is assigned to one or more levels."),
    LESSON_NOT_FOUND("lesson.not.found", "Lesson not found."),
    LESSON_STATUS_NOT_NEW("lesson.status.not.new", "This lesson has already started or finished."),
    LESSON_DELETE_RESTRICTION("lesson.delete.restriction", "Cannot delete a lesson that is already finished."),
    GROUP_NO_ENROLLMENT("group.no.active.enrollment", "Student does not have an active enrollment in this group."),
    REFERRAL_INVALID("group.referral.invalid", "Invalid or expired referral link."),
    REFERRAL_INACTIVE("group.referral.inactive", "This group is no longer accepting new students."),
    RESTORE_DELETED_BRANCH("group.restore.deleted.branch", "Cannot restore group while its branch is still deleted."),
    STUDENT_ALREADY_ENROLLED("group.student.already.enrolled", "Student is already a member of this group."),

    // 4. EXERCISES & AI
    EXERCISE_MAX_RETRIES("exercise.max.retries", "Maximum attempts reached. Please ask your teacher to unlock."),
    EXERCISE_PASSED("exercise.already.passed", "You have already passed this task."),
    EXERCISE_PENDING_REVIEW("exercise.pending.review", "You already have a submission pending review."),
    EXERCISE_WRITING_SHORT("exercise.writing.short", "Your essay is too short. Please write more words."),
    AI_DISABLED("exercise.ai.disabled", "AI evaluation is disabled for your school."),
    AI_SPEECH_FAILED("exercise.ai.speech.failed", "The AI could not recognize your speech. Please try again."),
    WORD_NOT_LINKED("exercise.word.not.linked", "This word is not linked to the selected set."),
    WORD_NOT_FOUND("exercise.word.not.found", "Vocabulary word not found."),
    PRONUNCIATION_MASTERED("exercise.pronunciation.mastered", "You have already mastered this word."),
    SECTION_TYPE_REQUIRED("exercise.section.type.required", "You must choose a Section Type."),

    // 5. EXAMS
    EXAM_NOT_STARTED("exam.not.started", "This exam has not started yet."),
    EXAM_CLOSED("exam.closed", "The exam window has closed."),
    EXAM_SESSION_NOT_FOUND("exam.session.not.found", "No active exam session found."),
    EXAM_TIME_EXPIRED("exam.section.time.expired", "Time has expired for this section."),
    EXAM_BLOCKED("exam.blocked", "Your exam session has been blocked."),

    // 6. FINANCE & PAYMENTS
    ORDER_NOT_FOUND("finance.order.not.found", "Payment order not found."),
    ORDER_ALREADY_PAID("finance.order.already.paid", "This order has already been paid."),
    GATEWAY_ERROR("finance.gateway.error", "Payment gateway error. Could not create payment link."),
    INSUFFICIENT_FUNDS("finance.insufficient.funds", "Student has insufficient funds."),
    PAYOUT_DETAILS_MISSING("finance.payout.missing.details", "Payout details not configured for this branch."),
    TRANSACTION_NOT_FOUND("finance.transaction.not.found", "Transaction record not found."),
    TRANSACTION_MISMATCH("finance.transaction.mismatch", "Amount mismatch error."),

    // 7. SHOP
    SHOP_ITEM_NOT_FOUND("shop.item.not.found", "Item not found."),
    SHOP_ITEM_INACTIVE("shop.item.inactive", "This item is currently not on sale."),
    SHOP_LOW_COINS("shop.low.coins", "You do not have enough coins for this purchase."),
    SHOP_OUT_OF_STOCK("shop.out.of.stock", "The available quantity is limited."),
    SHOP_WRONG_BRANCH("shop.wrong.branch", "This item is not available in your school shop."),
    SHOP_ORDER_CANCEL_FORBIDDEN("shop.order.cancel.forbidden", "You cannot cancel someone else's order."),
    // 8. SYSTEM
    FILE_EMPTY("system.file.empty", "File is empty."),
    FILE_TOO_LARGE("system.file.too.large", "File size exceeds the 25MB limit."),
    FILE_UPLOAD_ERROR("system.file.upload.error", "An error occurred during file upload."),
    URL_NOT_FOUND("system.url.not.found", "URL not found."),
    UNKNOWN_ERROR("system.error.unexpected", "An unexpected system error occurred."),
    DELETE_BLOCKED("system.delete.blocked", "Cannot delete: record is linked to other active data."),

    // 9. MISCELLANEOUS
    UNIT_NOT_FOUND("unit.not.found", "Unit not found."),
    UNIT_LOCKED("unit.locked", "This unit is currently locked."),
    MENTOR_NOT_FOUND("mentor.not.found", "Mentor not found."),
    MENTOR_ALREADY_ASSIGNED("mentor.already.assigned", "This mentor is already assigned to this school."),
    ATTACHMENT_NOT_FOUND("attachment.not.found", "File not found."),
    ORG_SETTINGS_LOCKED("org.settings.locked", "These settings are locked by your Organization."),
    DIRECTOR_NOT_FOUND("auth.director.not.found", "Director profile not found."),
    ORG_NOT_FOUND("org.not.found", "Organization not found."),

    // OLD

    PASSWORD_IS_INCORRECT("password.incorrect", "Parolingiz xato"),
    SCHOOL_BOOK_NOT_FOUND("school.book.not.found", "School book not found"),
    INVALID_TOKEN("invalid.token", "Token yaroqsiz"),
    SCHOOL_ADMIN_NOT_FOUND("school.admin.not.found", "Maktab administratori topilmadi"),
    MODERATOR_NOT_FOUND("moderator.not.found", "Moderator topilmadi"),
    FILE_SIZE_EXCEEDS("file.size.exceeds", "Fayl hajmi 5MBdan yuqori"),
    FILE_IS_EMPTY("file.is.empty", "Fayl bo'sh"),
    OLD_PASSWORD_IS_INCORRECT("old.password.incorrect", "Eski parol xato kiritildi"),
    NEW_PASSWORDS_DO_NOT_MATCH("new.passwords.do.not.match", "Yengi parollar bir-biriga mos kemadi"),
    MENTOR_ASSIGNMENT_NOT_FOUND("mentor.assignment.not.found", "Mentor assignment-i topilmadi"),
    SCHOOL_CLASS_NOT_FOUND("school.class.not.found", "Sinf topilmadi"),
    STUDENT_SEGMENT_NOT_STARTED("student.segment.not.started", "Student segment has not been started."),
    STUDENT_SEGMENT_NOT_PAUSED("student.segment.not.paused", "Student segment is not paused."),
    STUDENT_SEGMENT_ALREADY_STARTED("student.segment.already.started", "Student segment is already active."),
    STUDENT_SEGMENT_ALREADY_PAUSED("student.segment.already.paused", "Student segment is already paused."),
    STUDENT_SEGMENT_ALREADY_ENDED("student.segment.already.ended", "Student segment has already ended for this session."),
    SELLO_ERROR("sello.error", "Sello bilan xatolik yuzaga keldi" ),
    
// Entity Specifics
    VOCAB_SET_NOT_FOUND("exercise.vocab.set.not.found", "Vocabulary set not found."),
    EXERCISE_QUESTION_NOT_FOUND("exercise.question.not.found", "Question not found."),
    EXERCISE_TASK_NOT_FOUND("exercise.task.not.found", "Task not found."),
    EXERCISE_SUBMISSION_NOT_FOUND("exercise.submission.not.found", "Submission not found."),
    SUBSCRIPTION_PLAN_NOT_FOUND("school.plan.not.found", "Subscription plan not found."),
    SCHEDULE_NOT_FOUND("course.schedule.not.found", "Schedule not found."),
    SHOP_ORDER_NOT_FOUND("shop.order.not.found", "The requested shop order was not found."),
    PARENT_CONTACT_REQUIRED("parent.contact.required", "Parent must have a Telegram nickname or phone number provided."),
    EXAM_SETTINGS_NOT_FOUND("school.exam.settings.not.found", "Exam settings not configured for this branch."),
    UNIT_NOT_SCHEDULED("course.unit.not.scheduled", "This unit is not scheduled for your group."),
    PAYMENT_RECORD_NOT_FOUND("finance.payment.record.not.found", "Payment record not found."),
    BOOKMARK_NOT_FOUND("exercise.bookmark.not.found", "Bookmark not found."),
    RECORDING_NOT_FOUND("course.recording.not.found", "Lesson recording not found."),
    FOLDER_NOT_FOUND("library.folder.not.found", "Parent folder or directory not found."),
    PAYOUT_NOT_CONFIGURED("finance.payout.not.configured", "Payout details not configured for this branch."),
    SUBSCRIPTION_SETTINGS_NOT_FOUND("school.subscription.settings.not.found", "Subscription settings not found for this school branch."),
    LIBRARY_ITEM_NOT_FOUND("library.item.not.found", "Library item not found."),

    SYSTEM_WEBHOOK_INVALID("system.webhook.invalid", "Invalid webhook payload."),
    LIST_EMPTY("system.list.empty", "Request list cannot be empty."),
    PERMISSION_EXISTS("system.permission.exists", "Permission already exists."),
    BRANCH_HAS_GROUPS("school.branch.has.groups", "Branch has active groups."),
    EXERCISE_LIMIT_ONE("exercise.limit.one", "Limit: One Speaking/Writing per task."),
    PRONUNCIATION_INVALID("exercise.pronunciation.invalid", "Not a pronunciation task."),
    INVALID_ARGUMENT("common.invalid.argument", "Invalid argument provided."),
    EXAM_POLICY_TIMER_DISABLED("exam.policy.timer.disabled", "This school's policy does not use separate section timers."),
    EXCEL_UPLOAD_SUCCESS("excel.upload.success", "Excel file uploaded successfully."),
    PARENT_NAME_REQUIRED("parent.name.required", "Parent name is required."),
    // Inside MessageKey.java
    REFERRAL_ALREADY_ACTIVE("auth.referral.already.active", "Student is already active."),
    REFERRAL_NOT_PENDING("auth.referral.not.pending", "Student is not awaiting referral approval."),
    SCORE_OUT_OF_RANGE("exercise.score.range", "Score must be between 0 and 100."),
    EXAM_SETTINGS_LOCKED("school.exam.settings.locked", "Exam settings are locked. Please contact your Organization Director."),
    HOMEWORK_ATTEMPT_LIMIT_REACHED("homework.attempt.limit.reached", "You have reached the homework attempt limit for this task."),
    PARENT_NOT_FOUND("auth.parent.not.found", "No parent is registered with this phone number."),
    PARENT_ACCESS_ONLY("auth.parent.access.only", "This action is only available to parents."),
    TEACHER_NOT_FOUND("auth.teacher.not.found", "Teacher not found."),
    TEACHER_SALARY_PLAN_NOT_FOUND("finance.teacher.salary.plan.not.found", "Salary plan not configured for this teacher."),
    TEACHER_SALARY_PLAN_EXISTS("finance.teacher.salary.plan.exists", "This teacher already has a salary plan. Update it instead of creating a new one."),
    STUDENT_DISCOUNT_NOT_FOUND("finance.student.discount.not.found", "Discount not found."),
    STUDENT_DISCOUNT_TYPE_EXISTS("finance.student.discount.type.exists", "This student already has an active discount of this type. Update it instead of creating a new one.");

    private final String key;
    private final String value;
}