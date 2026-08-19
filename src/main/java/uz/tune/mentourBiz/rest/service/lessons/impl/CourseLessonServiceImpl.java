package uz.tune.mentourBiz.rest.service.lessons.impl;
import uz.tune.mentourBiz.rest.enums.MessageKey;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.SchoolBook;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.postExercise.UnitProgress;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.Course;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.CourseLesson;
import uz.tune.mentourBiz.rest.domain.schoolManagement.schedule.AttendanceRecord;
import uz.tune.mentourBiz.rest.domain.schoolManagement.schedule.GroupSchedule;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.*;
import uz.tune.mentourBiz.rest.payload.req.course.ReqLesson;
import uz.tune.mentourBiz.rest.payload.req.course.ReqLessonUnitAssignment;
import uz.tune.mentourBiz.rest.payload.req.course.ReqLessonUpdateItem;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.course.ResLessonDetail;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResCreateLessons;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResStartMeeting;
import uz.tune.mentourBiz.rest.repository.course.CourseLessonRepo;
import uz.tune.mentourBiz.rest.repository.course.CourseRepo;
import uz.tune.mentourBiz.rest.repository.group.GroupRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolBookRepository;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.schedule.AttendanceRecordRepository;
import uz.tune.mentourBiz.rest.repository.schedule.GroupScheduleRepository;
import uz.tune.mentourBiz.rest.repository.unit.UnitRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.UnitProgressRepository;
import uz.tune.mentourBiz.rest.repository.user.TeacherRepository;
import uz.tune.mentourBiz.rest.service.group.schedule.ScheduleService;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.rest.service.lessons.CourseLessonService;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;
import uz.tune.mentourBiz.rest.service.util.MessageSingleton;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseLessonServiceImpl implements CourseLessonService {

    private final CourseLessonRepo courseLessonRepo;
    private final CourseRepo courseRepo;
    private final MessageSingleton messageSingleton;
    private final UserService userService;
    private final GroupScheduleRepository groupScheduleRepository;
    private final UserScopeService userScopeService;
    private final UnitProgressRepository unitProgressRepository;
    private final UnitRepository unitRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TeacherRepository teacherRepository;
    private final ScheduleService scheduleService;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AuthToViewEntity authToViewEntity;
    private final GroupRepository groupRepository;
    private final SchoolBookRepository schoolBookRepository;

    @Override
    @Transactional
    public ResStartMeeting startLessonMeeting(UUID lessonId) {
        User currentUser = userService.getCurrentUser();

        CourseLesson lesson = updateLessonStatusToStarted(lessonId);



        String bbbRole = "MODERATOR";
//        String joinUrl = bigBlueButtonService.getRawBbbJoinUrl(
//                bbbMeetingId,
//                currentUser.getFirstName() + " " + currentUser.getLastName(),
//                currentUser.getUuid(),
//                bbbRole
//        );
        return new ResStartMeeting(lesson.getUuid(), lesson.getName(), null, "Meeting started");
    }

    public CourseLesson updateLessonStatusToStarted(UUID lessonId) {
        CourseLesson lesson = courseLessonRepo.findByUuid(lessonId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.LESSON_NOT_FOUND.getKey()));

        authorizeLessonAction(lesson);

        if (!lesson.getStatus().equals(LessonStatus.NEW)) {
            throw new ValidationException(MessageKey.LESSON_STATUS_NOT_NEW.getKey());
        }

        lesson.setStatus(LessonStatus.STARTED);
        return courseLessonRepo.save(lesson);
    }


    private void authorizeLessonAction(CourseLesson lesson) {
        User currentUser = userService.getCurrentUser();
        if (currentUser.getRole().equals(UserRole.SYS_ADMIN)) {
            return;
        }
        UUID userSchoolUuid = userScopeService.getCurrentUserSchoolUuid();

        if (!lesson.getCourse().getSchool().getUuid().equals(userSchoolUuid)
                || UserRole.STUDENT.equals(currentUser.getRole())) {
            throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
        }
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ResLessonDetail> getLessonsForCourse(UUID courseId, Pageable pageable,
                                                     List<LessonStatus> lessonStatus,
                                                     String lessonName,
                                                     LocalDate date) {
        User currentUser = userService.getCurrentUser();
        Course course = courseRepo.findByUuid(courseId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.COURSE_NOT_FOUND.getKey()));

        authToViewEntity.authorizeUponCourse(currentUser, course);

        Instant startInstant = null;
        Instant endInstant = null;
        if (date != null) {
            ZoneId zone = schoolZone(course);
            startInstant = date.atStartOfDay(zone).toInstant();
            endInstant = date.atTime(LocalTime.MAX).atZone(zone).toInstant();
        }

        Page<CourseLesson> lessonsPage = courseLessonRepo.findWithFilters(
                lessonName,
                lessonStatus,
                null,
                null,
                List.of(courseId),
                startInstant,
                endInstant,
                pageable
        );

        return lessonsPage.map(ResLessonDetail::new);
    }

    @Override
    public ResLessonDetail getLessonById(UUID lessonId) {
//        recordingService.triggerRecordingCheckAsync();
        User currentUser = userService.getCurrentUser();
        CourseLesson lesson = auth(lessonId, currentUser);


//        if (CoreUtils.isPresent(lesson.getBbbMeetingId())) {
//            joinLink = bigBlueButtonService.getRawBbbJoinUrl(
//                    lesson.getBbbMeetingId(),
//                    currentUser.getFirstName() + " " + currentUser.getLastName(),
//                    currentUser.getUuid(),
//                    bbbRole
//            );
//        }
//        detail.setLink(null);
        return new ResLessonDetail(lesson);
    }



    @Override
    @Transactional
    public ResCreateLessons createLessons(UUID courseId, List<ReqLesson> requestList) {
        Course course = courseRepo.findByUuid(courseId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.COURSE_NOT_FOUND.getKey()));

        ZoneId zone = schoolZone(course);
        Map<UUID, LessonStatus> result = new HashMap<>();

        Set<UUID> existingUnitUuids = course.getLessons().stream()
                .filter(l -> l.getStatus() != LessonStatus.DELETED)
                .flatMap(l -> l.getUnits().stream())
                .map(Unit::getUuid)
                .collect(Collectors.toSet());

        List<Enrollment> enrollments = enrollmentRepository.findAllByGroup_UuidAndStatus(
                course.getGroup().getUuid(), EnrollmentStatus.ONGOING);

        for (ReqLesson req : requestList) {
            if (req.getUnitUuids() != null) {
                for (UUID newUnitUuid : req.getUnitUuids()) {
                    if (existingUnitUuids.contains(newUnitUuid)) {
                        Unit unit = unitRepository.findByUuid(newUnitUuid).orElse(null);
                        String unitName = (unit != null) ? unit.getTitle() : newUnitUuid.toString();
                        throw new ValidationException(MessageKey.UNIT_DUPLICATE_IN_COURSE.getKey() + unitName + "' is already assigned to another lesson in this course.");
                    }
                    existingUnitUuids.add(newUnitUuid);
                }
            }

            CourseLesson lesson = createLessonFromRequest(req, course, zone);

            if (req.getUnitUuids() != null && !req.getUnitUuids().isEmpty()) {
                List<Unit> units = unitRepository.findAllByUuidIn(req.getUnitUuids());
                lesson.setUnits(units);
                courseLessonRepo.save(lesson);

                createScheduleForLesson(course.getGroup(), lesson);

                scheduleService.createUnitsProgressesForNewSchedule(course.getGroup().getUuid(), units);
            } else {
                courseLessonRepo.save(lesson);
                createScheduleForLesson(course.getGroup(), lesson);
            }

            createInitialAttendanceRecords(lesson, enrollments);

            result.put(lesson.getUuid(), lesson.getStatus());
        }
        return new ResCreateLessons(result);
    }


    // Every student enrolled at the moment the lesson is created starts as NOT_MARKED,
    // even when the lesson is created with a past date. Students enrolled later (transfer)
    // get no record for it, which the attendance grid renders as an empty cell.
    private void createInitialAttendanceRecords(CourseLesson lesson, List<Enrollment> enrollments) {
        // Shu darsga allaqachon yozuvi bor studentlarni o'tkazib yuboramiz, aks holda dublikat
        // paydo bo'lib, davomat/baholar jadvalidagi toMap "Duplicate key" bilan yiqiladi.
        Set<Long> alreadyHasRecord = attendanceRecordRepository.findAllByLesson(lesson).stream()
                .map(a -> a.getStudent().getId())
                .collect(Collectors.toSet());

        List<AttendanceRecord> records = new ArrayList<>();
        for (Enrollment e : enrollments) {
            if (!alreadyHasRecord.add(e.getStudent().getId())) continue;
            AttendanceRecord ar = new AttendanceRecord();
            ar.setUuid(UUID.randomUUID());
            ar.setLesson(lesson);
            ar.setStudent(e.getStudent());
            ar.setStatus(AttendanceStatus.NOT_MARKED);
            ar.setIsMarked(false);
            ar.setIsAttendanceNotified(false);
            ar.setIsUnitNotified(false);
            records.add(ar);
        }
        attendanceRecordRepository.saveAll(records);
    }

    // Lesson dates/times arrive as the school's local wall clock, so they must be converted to
    // Instant with that school's own UTC offset. Tashkent (+5) is only the fallback when the
    // school has no offset set.
    private ZoneId schoolZone(Course course) {
        Integer utcOffset = course.getSchool() != null ? course.getSchool().getUtcOffset() : null;
        return utcOffset != null ? ZoneOffset.ofHours(utcOffset) : ZoneId.of("Asia/Tashkent");
    }

    private CourseLesson createLessonFromRequest(ReqLesson request, Course course, ZoneId zoneId) {
        CourseLesson lesson = new CourseLesson();
        lesson.setCourse(course);
        lesson.setName(request.getName());
        lesson.setStartTime(LocalDateTime.of(LocalDate.parse(request.getDate()), LocalTime.parse(request.getStartTime())).atZone(zoneId).toInstant());
        lesson.setEndTime(LocalDateTime.of(LocalDate.parse(request.getDate()), LocalTime.parse(request.getEndTime())).atZone(zoneId).toInstant());
        lesson.setLessonType(request.getLessonType());
        lesson.setDescription(request.getDescription());
        lesson.setStatus(LessonStatus.STUDENT_APP);
        return lesson;
    }

    private void initializeStudentProgress(Group group, Unit unit) {
        List<Enrollment> enrollments = enrollmentRepository.findAllByGroup_UuidAndStatusAndStudent_User_Status(
                group.getUuid(), EnrollmentStatus.ONGOING, UserStatus.ACTIVE);

        for (Enrollment e : enrollments) {
            if (unitProgressRepository.findByStudentAndUnit(e.getStudent(), unit).isEmpty()) {
                UnitProgress up = new UnitProgress();
                up.setStudent(e.getStudent());
                up.setUnit(unit);
                up.setProgressPercentage(0);
                up.setStatus(UnitProgressStatus.ATTEMPTED);
                unitProgressRepository.save(up);
            }
        }
    }


    @Override
    @Transactional
    public ResponseMessage updateLessons(UUID courseId, List<ReqLessonUpdateItem> requestList) {
        Course course = courseRepo.findByUuid(courseId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.COURSE_NOT_FOUND.getKey()));

        ZoneId uzZone = schoolZone(course);

        for (ReqLessonUpdateItem req : requestList) {
            CourseLesson lesson = courseLessonRepo.findByUuid(req.getLessonId())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.LESSON_NOT_FOUND.getKey()));


            if (req.getUnitUuids() != null) {
                Set<UUID> unitsInOtherLessons = course.getLessons().stream()
                        .filter(l -> !l.getStatus().equals(LessonStatus.DELETED))
                        .filter(l -> !l.getUuid().equals(lesson.getUuid()))
                        .flatMap(l -> l.getUnits().stream())
                        .map(Unit::getUuid)
                        .collect(Collectors.toSet());


                for (UUID requestedUnitUuid : req.getUnitUuids()) {
                    if (unitsInOtherLessons.contains(requestedUnitUuid)) {
                        Unit conflictUnit = unitRepository.findByUuid(requestedUnitUuid).orElse(null);
                        String unitName = (conflictUnit != null) ? conflictUnit.getTitle() : requestedUnitUuid.toString();

                        throw new ValidationException(MessageKey.UNIT_DUPLICATE_IN_COURSE.getKey() + unitName +
                                "' is already assigned to another lesson in this course. Each unit can only be linked once per course.");
                    }
                }

                List<Unit> newUnits = unitRepository.findAllByUuidIn(req.getUnitUuids());
                lesson.setUnits(newUnits);

                Instant newEndTime = LocalDateTime.parse(req.getDate() + "T" + req.getEndTime())
                        .atZone(uzZone).toInstant();

                groupScheduleRepository.findByLesson(lesson).ifPresent(s -> {
                    s.setDueDate(newEndTime);
                    if (!newUnits.isEmpty()) {
                        s.setUnit(newUnits.get(0));
                    }
                    groupScheduleRepository.save(s);
                });

                if (course.getGroup() != null) {
                    scheduleService.createUnitsProgressesForNewSchedule(course.getGroup().getUuid(), newUnits);
                }
            }

            if(req.getName() != null) {
                lesson.setName(req.getName());
            }
            if(req.getDescription() != null) {
                lesson.setDescription(req.getDescription());
            }
            if(req.getStartTime() != null) {
                lesson.setStartTime(LocalDateTime.parse(req.getDate() + "T" + req.getStartTime())
                        .atZone(uzZone).toInstant());
            }
            if(req.getEndTime() != null) {
                lesson.setEndTime(LocalDateTime.parse(req.getDate() + "T" + req.getEndTime())
                        .atZone(uzZone).toInstant());
            }
            courseLessonRepo.save(lesson);
        }

        return new ResponseMessage("Lessons updated successfully with zero unit duplicates.");
    }


    @Override
    @Transactional
    public ResponseMessage assignBookUnitsToLessons(UUID groupId, UUID schoolBookId,
                                                    List<ReqLessonUnitAssignment> assignments) {
        Group group = groupRepository.findByUuid(groupId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.GROUP_NOT_FOUND.getKey()));

        SchoolBook book = schoolBookRepository.findByUuid(schoolBookId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_BOOK_NOT_FOUND.getKey()));

        // Lessons that belong to this group (excluding deleted ones), indexed by uuid.
        List<CourseLesson> groupLessons = courseLessonRepo.findAllByCourse_Group_UuidAndStatusIn(
                group.getUuid(),
                List.of(LessonStatus.NEW, LessonStatus.STARTED, LessonStatus.FINISHED, LessonStatus.STUDENT_APP));
        Map<UUID, CourseLesson> lessonByUuid = groupLessons.stream()
                .collect(Collectors.toMap(CourseLesson::getUuid, l -> l));

        // Units that belong to the requested book, indexed by uuid.
        Map<UUID, Unit> unitByUuid = unitRepository.findAllBySchoolBookUuid(book.getUuid()).stream()
                .collect(Collectors.toMap(Unit::getUuid, u -> u));

        // Lessons being (re)assigned in this request — their current units may be replaced freely.
        Set<UUID> targetLessonIds = assignments.stream()
                .map(ReqLessonUnitAssignment::getLessonId)
                .collect(Collectors.toSet());

        // Units already linked to the group's other (untouched) lessons — a unit can only live in one lesson.
        Set<UUID> unitsInUntouchedLessons = groupLessons.stream()
                .filter(l -> !targetLessonIds.contains(l.getUuid()))
                .flatMap(l -> l.getUnits().stream())
                .map(Unit::getUuid)
                .collect(Collectors.toSet());

        Set<UUID> seenUnits = new HashSet<>();

        // ---- validate everything before mutating anything ----
        for (ReqLessonUnitAssignment a : assignments) {
            CourseLesson lesson = lessonByUuid.get(a.getLessonId());
            if (lesson == null) {
                throw new ValidationException(MessageKey.LESSON_NOT_FOUND.getKey() + " " + a.getLessonId()
                        + " does not belong to group " + group.getUuid());
            }
            if (a.getUnitUuids() == null) continue;

            for (UUID unitUuid : a.getUnitUuids()) {
                Unit unit = unitByUuid.get(unitUuid);
                if (unit == null) {
                    throw new ValidationException(MessageKey.UNIT_NOT_FOUND.getKey() + " " + unitUuid
                            + " is not part of book " + book.getName());
                }
                if (!seenUnits.add(unitUuid) || unitsInUntouchedLessons.contains(unitUuid)) {
                    throw new ValidationException(MessageKey.UNIT_DUPLICATE_IN_COURSE.getKey() + " '"
                            + unit.getTitle() + "' can only be linked to one lesson in this group.");
                }
            }
        }

        // ---- apply ----
        for (ReqLessonUnitAssignment a : assignments) {
            CourseLesson lesson = lessonByUuid.get(a.getLessonId());
            List<Unit> units = (a.getUnitUuids() == null)
                    ? new ArrayList<>()
                    : a.getUnitUuids().stream().map(unitByUuid::get).collect(Collectors.toList());

            lesson.setUnits(units);
            courseLessonRepo.save(lesson);

            groupScheduleRepository.findByLesson(lesson).ifPresent(s -> {
                s.setUnit(units.isEmpty() ? null : units.get(0));
                groupScheduleRepository.save(s);
            });

            if (!units.isEmpty()) {
                scheduleService.createUnitsProgressesForNewSchedule(group.getUuid(), units);
            }
        }

        return new ResponseMessage("Units assigned to lessons successfully.");
    }


    private void createScheduleForLesson(Group group, CourseLesson lesson) {
        Optional<GroupSchedule> existing = groupScheduleRepository.findByLesson(lesson);
        if (existing.isPresent()) return;

        GroupSchedule schedule = new GroupSchedule();
        schedule.setGroup(group);
        schedule.setLesson(lesson);
        schedule.setDueDate(lesson.getEndTime());
        schedule.setStatus(GroupScheduleStatus.ACTIVE);
        if (!lesson.getUnits().isEmpty()) {
            schedule.setUnit(lesson.getUnits().get(0));
        }
        groupScheduleRepository.save(schedule);
    }


    @Override
    @Transactional
    public ResponseMessage deleteLesson(UUID lessonUuid) {
        User currentUser = userService.getCurrentUser();
        CourseLesson lesson = auth(lessonUuid, currentUser);

        lesson.setStatus(LessonStatus.DELETED);
        courseLessonRepo.save(lesson);

        List<GroupSchedule> schedules = groupScheduleRepository.findAllByLesson(lesson);
        schedules.forEach(gs -> gs.setStatus(GroupScheduleStatus.DELETED));
        groupScheduleRepository.saveAll(schedules);

        return new ResponseMessage("Lesson and all associated unit schedules deleted");
    }


    @Override
    @Transactional
    public ResponseMessage undeleteLesson(UUID lessonUuid) {
        User currentUser = userService.getCurrentUser();
        CourseLesson lesson = auth(lessonUuid, currentUser);

        lesson.setStatus(LessonStatus.STUDENT_APP);
        groupScheduleRepository.findByLesson(lesson).ifPresent(gs -> {
            gs.setStatus(GroupScheduleStatus.ACTIVE);
            groupScheduleRepository.save(gs);
        });
        courseLessonRepo.save(lesson);
        return new ResponseMessage("Lesson restored");
    }



    public CourseLesson auth(UUID lessonUuid, User currentUser) {
        CourseLesson lesson = courseLessonRepo.findByUuid(lessonUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.LESSON_NOT_FOUND.getKey()));

        authToViewEntity.authorizeUponCourse(currentUser, lesson.getCourse());

        if (lesson.getStatus().equals(LessonStatus.DELETED) && currentUser.getRole().equals(UserRole.STUDENT)) {
            throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
        }

        return lesson;
    }

    private void validateUserAccess(Course course, User currentUser) {
        if (currentUser.getRole().equals(UserRole.SYS_ADMIN)) return;

        if (currentUser.getRole().equals(UserRole.MENTOR)) {
            if (!course.getMentor().getUser().equals(currentUser)) throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
        } else if (currentUser.getRole().equals(UserRole.TEACHER)) {
            Teacher teacher = teacherRepository.findByUser_Uuid(currentUser.getUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
            if (!course.getGroup().getTeacher().equals(teacher)) throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
        } else {
            UUID schoolUuid = userScopeService.getCurrentUserSchoolUuid();
            if (!course.getSchool().getUuid().equals(schoolUuid)) throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
        }
    }
}