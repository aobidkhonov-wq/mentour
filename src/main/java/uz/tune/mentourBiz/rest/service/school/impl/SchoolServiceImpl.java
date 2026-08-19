package uz.tune.mentourBiz.rest.service.school.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uz.tune.mentourBiz.config.logger.Logger;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.*;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.SchoolBook;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.Branch;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.userManagement.user.SchoolMentor;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.*;
import uz.tune.mentourBiz.rest.payload.req.ReqSchoolExamSettings;
import uz.tune.mentourBiz.rest.payload.req.school.ReqSchoolCreate;
import uz.tune.mentourBiz.rest.payload.req.school.ReqSchoolUpdate;
import uz.tune.mentourBiz.rest.payload.req.school.ReqUpdateMentorHours;
import uz.tune.mentourBiz.rest.payload.res.ResBookCoinStats;
import uz.tune.mentourBiz.rest.payload.res.ResSchoolExamSettings;
import uz.tune.mentourBiz.rest.payload.res.ResSchoolSubscription;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.school.ResMentorsForSchool;
import uz.tune.mentourBiz.rest.payload.res.school.ResSchoolInfo;
import uz.tune.mentourBiz.rest.repository.*;
import uz.tune.mentourBiz.rest.repository.course.CourseLessonRepo;
import uz.tune.mentourBiz.rest.repository.course.CourseRepo;
import uz.tune.mentourBiz.rest.repository.group.GroupRepository;
import uz.tune.mentourBiz.rest.repository.group.LevelRepository;
import uz.tune.mentourBiz.rest.repository.schedule.GroupScheduleRepository;
import uz.tune.mentourBiz.rest.repository.school.BranchRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolBookRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolMentorRepo;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.user.SchoolAdminRepo;
import uz.tune.mentourBiz.rest.repository.user.TeacherRepository;
import uz.tune.mentourBiz.rest.repository.user.UserRepo;
import uz.tune.mentourBiz.rest.service.SubscriptionValidator;
import uz.tune.mentourBiz.rest.service.group.GroupServiceImpl;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.rest.service.lessons.CourseService;
import uz.tune.mentourBiz.rest.service.school.SchoolService;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;
import uz.tune.mentourBiz.rest.service.util.MessageSingleton;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SchoolServiceImpl implements SchoolService {

    private final SchoolRepo schoolRepo;
    private final AttachmentRepo attachmentRepo;
    
    private final SchoolMentorRepo schoolMentorRepo;
    private final MessageSingleton messageSingleton;
    private final CourseRepo courseRepo;
    private final CourseService courseService;
    private final SchoolAdminRepo schoolAdminRepo;
    
    private final UserRepo userRepo;
    private final UserService userService;
    private final UserScopeService userScopeService;
    private final StudentRepo studentRepo;
    private final BranchRepository branchRepository;
    private final AuthToViewEntity authToViewEntity;
    private final GroupRepository groupRepository;
    private final GroupServiceImpl groupService;
    private final TeacherRepository teacherRepository;
    private final SchoolBookRepository schoolBookRepository;
    private final static Integer LEVELS_COUNTS = 6;
    private final LevelRepository levelRepository;
    private final SubscriptionPlanRepo subscriptionPlanRepo;
    private final SubscriptionValidator subscriptionValidator;
    private final SchoolExamSettingsRepo schoolExamSettingsRepo;
    private final RegionRepository regionRepository;
    private final SchoolSubscriptionRepo schoolSubscriptionRepo;
    private final SchoolAcademicConfigRepo schoolAcademicConfigRepo;
    private final SchoolDirectorRepo schoolDirectorRepo;
    private final CourseLessonRepo courseLessonRepo;
    private final GroupScheduleRepository groupScheduleRepository;
    private final SchoolRewardConfigRepo schoolRewardConfigRepo;

    @Override
    public Page<ResSchoolInfo> getAllSchools(Pageable pageable, SchoolStatus status, String schoolName) {
        User currentUser = userService.getCurrentUser();

        Collection<UUID> scopeUuids = userScopeService.getAuthorizedSchoolUuids();

        Page<School> schoolPage = schoolRepo.findWithFilters(status, schoolName, scopeUuids, pageable);

        return schoolPage.map(s -> {
            SchoolSubscription sub = schoolSubscriptionRepo.findBySchool_Uuid(s.getUuid()).orElse(null);
            ResSchoolInfo dto = new ResSchoolInfo(s, new ResSchoolSubscription(s, sub));

            dto.setLastLessonCreatedAt(courseLessonRepo.findLastLessonCreatedAtBySchool(s.getUuid()));
            dto.setLatestDueDate(groupScheduleRepository.findLatestDueDateBySchool(s.getUuid()));

            return dto;
        });
    }

    @Override
    public List<ResSchoolInfo> getAllSchoolsList() {
        User currentUser = userService.getCurrentUser();
        List<School> schoolPage;

        if (currentUser.getRole().equals(UserRole.SYS_ADMIN)) {
            schoolPage = schoolRepo.findAll();
        } else {
            UUID schoolUuid = userScopeService.getCurrentUserSchoolUuid();
            schoolPage = schoolRepo.findAllByUuidAndStatus(schoolUuid, SchoolStatus.ACTIVE);
        }
        return schoolPage.stream()
                .map(s -> {
                    SchoolSubscription sub = schoolSubscriptionRepo
                            .findBySchool_Uuid(s.getUuid())
                            .orElse(null);

                    return new ResSchoolInfo(
                            s,
                            new ResSchoolSubscription(s, sub)
                    );
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ResSchoolInfo> getDirectorSchools() {
        User user = userService.getCurrentUser();
        if (!user.getRole().equals(UserRole.SCHOOL_DIRECTOR)) return List.of();

        return schoolDirectorRepo.findByUser(user)
                .map(director -> director.getOrganization().getSchools().stream()
                        .map(school -> {
                            SchoolSubscription sub = schoolSubscriptionRepo.findBySchool_Uuid(school.getUuid()).orElse(null);
                            ResSchoolSubscription resSub = (sub != null) ? new ResSchoolSubscription(school, sub) : null;

                            return new ResSchoolInfo(school, resSub);
                        }).collect(Collectors.toList()))
                .orElse(List.of());
    }

    @Override
    @Transactional
    public ResponseMessage createSchool(ReqSchoolCreate request) {
        User user = userService.getCurrentUser();
        // 1. Basic School Information
        School school = new School();
        school.setName(request.getName());
        school.setAddress(request.getAddress());
        school.setStatus(SchoolStatus.ACTIVE);

        if(CoreUtils.isPresent(request.getPhone())) {
            school.setContactInfo(request.getPhone());
        }
        if(CoreUtils.isPresent(request.getTelegramLink())) {
            school.setTelegramLink(request.getTelegramLink());
        }

        // 2. Handle Logo Attachment
        if (CoreUtils.isPresent(request.getLogoId())) {
            Attachment logo = attachmentRepo.findByUuid(request.getLogoId())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.ATTACHMENT_NOT_FOUND.getKey()));
            school.setLogo(logo);
        }
        // 3. Set Region
        if (request.getRegionUuid() != null) {
            Region region = regionRepository.findByUuid(request.getRegionUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.REGION_NOT_FOUND.getKey()));
            school.setRegion(region);
            school.setUtcOffset(region.getTimeUtc());
        }

        // 4. Set Subscription Plan (Only SysAdmins can pick, others get DEFAULT)
        if (request.getPlanUuid() != null && user.getRole().equals(UserRole.SYS_ADMIN)) {
            SubscriptionPlan plan = subscriptionPlanRepo.findByUuid(request.getPlanUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.SUBSCRIPTION_PLAN_NOT_FOUND.getKey()));
            school.setSubscriptionPlan(plan);
        } else {
            subscriptionPlanRepo.findByName("DEFAULT_PLAN").ifPresent(school::setSubscriptionPlan);
        }

        // 5. Assign Books
        if (request.getSchoolBookUuids() == null || request.getSchoolBookUuids().isEmpty()) {
            throw new ValidationException(MessageKey.SCHOOL_BOOKS_REQUIRED.getKey());
        }
        List<SchoolBook> schoolBooks = schoolBookRepository.findAllByUuidIn(request.getSchoolBookUuids());
        school.setAllowedBooks(schoolBooks);

        // Save School to generate ID for child entities
        School savedSchool = schoolRepo.saveAndFlush(school);

        // 6. Initialize Subscription record
        SchoolSubscription sub = new SchoolSubscription();
        sub.setSchool(savedSchool);
        sub.setExpiresAt(request.getExpiresAt() != null ? request.getExpiresAt() : java.time.Instant.now().plus(java.time.Duration.ofDays(30)));
        sub.setAiExerciseEnabled(true);
        sub.setAiWritingEnabled(true);
        sub.setAiSpeakingEnabled(true);
        schoolSubscriptionRepo.save(sub);

        // 7. Initialize Academic Config (Thresholds and Limits)
        SchoolAcademicConfig academicConfig = new SchoolAcademicConfig();
        academicConfig.setSchool(savedSchool);
        academicConfig.setMinScoreToPass(70);
        academicConfig.setMaxRetries(2);
        academicConfig.setTeacherMonthlyCoinLimit(5000L);
        schoolAcademicConfigRepo.save(academicConfig);

        // 8. Initialize Exam Settings (Lockdown Policy)
        SchoolExamSettings examSettings = new SchoolExamSettings();
        examSettings.setSchool(savedSchool);
        examSettings.setNoScreenshot(true);
        examSettings.setSeparateSection(false);
        examSettings.setAttemptLimit(1);
        examSettings.setFreezeScreen(true);
        examSettings.setFreezeTimer(120);
        examSettings.setTimeLimit(60);
        schoolExamSettingsRepo.save(examSettings);

        // 9. Initialize REWARD CONFIG (Centralized Coin Logic)
        SchoolRewardConfig rewardConfig = new SchoolRewardConfig();
        rewardConfig.setSchool(savedSchool);
        rewardConfig.setExerciseAutoEnabled(true); // Default to ON
        rewardConfig.setVocabAutoEnabled(true);     // Default to ON
        rewardConfig.setGapFillBase(3);
        rewardConfig.setOrderingBase(1);
        rewardConfig.setMatchingBase(2);
        rewardConfig.setSelectionBase(5);
        rewardConfig.setMultiSelectBase(3);
        rewardConfig.setCircleBase(3);
        rewardConfig.setTracingBase(2);
        rewardConfig.setAudioMultiplierEnabled(true);
        rewardConfig.setAudioMultiplier(1.67); // Ratio of 5/3 for audio tasks
        schoolRewardConfigRepo.save(rewardConfig);

        // 10. Create Default Branch (Required for Groups/Classes)
        Branch branch = new Branch();
        branch.setSchool(savedSchool);
        branch.setName(savedSchool.getName() + " Primary Branch");
        branch.setAddress(savedSchool.getAddress());
        branch.setStatus(BranchStatus.ACTIVE);
        branchRepository.save(branch);

        return new ResponseMessage("School created successfully. All modules and dynamic rewards are enabled.");
    }

    @Override
    @Transactional
    public ResponseMessage backfillDefaults() {
        List<School> schools = schoolRepo.findAll();
        int rewardsCreated = 0;
        int academicCreated = 0;
        int examCreated = 0;
        int subsCreated = 0;

        for (School school : schools) {
            // 1. Reward Configuration (The Coin Logic)
            if (schoolRewardConfigRepo.findBySchool_Uuid(school.getUuid()).isEmpty()) {
                SchoolRewardConfig rc = new SchoolRewardConfig();
                rc.setSchool(school);
                rc.setExerciseAutoEnabled(true); // TURN ON by default
                rc.setVocabAutoEnabled(true);    // TURN ON by default
                rc.setGapFillBase(3);
                rc.setOrderingBase(1);
                rc.setMatchingBase(2);
                rc.setSelectionBase(5);
                rc.setMultiSelectBase(3);
                rc.setCircleBase(3);
                rc.setTracingBase(2);
                rc.setAudioMultiplierEnabled(true);
                rc.setAudioMultiplier(1.67);
                schoolRewardConfigRepo.save(rc);
                rewardsCreated++;
            }

            // 2. Academic Config
            if (schoolAcademicConfigRepo.findBySchool_Uuid(school.getUuid()).isEmpty()) {
                SchoolAcademicConfig ac = new SchoolAcademicConfig();
                ac.setSchool(school);
                schoolAcademicConfigRepo.save(ac);
                academicCreated++;
            }

            // 3. Exam Settings
            if (schoolExamSettingsRepo.findBySchool_Uuid(school.getUuid()).isEmpty()) {
                SchoolExamSettings es = new SchoolExamSettings();
                es.setSchool(school);
                es.setNoScreenshot(true);
                es.setTimeLimit(60);
                schoolExamSettingsRepo.save(es);
                examCreated++;
            }

            // 4. Subscription
            if (schoolSubscriptionRepo.findBySchool_Uuid(school.getUuid()).isEmpty()) {
                SchoolSubscription sub = new SchoolSubscription();
                sub.setSchool(school);
                sub.setExpiresAt(java.time.Instant.now().plus(java.time.Duration.ofDays(30)));
                schoolSubscriptionRepo.save(sub);
                subsCreated++;
            }
        }

        return new ResponseMessage(String.format(
                "Backfill Success: Rewards(%d), Academic(%d), Exams(%d), Subs(%d)",
                rewardsCreated, academicCreated, examCreated, subsCreated
        ));
    }


    @Override
    @Transactional
    public ResponseMessage updateSchool(UUID schoolId, ReqSchoolUpdate request) {
        School school = schoolRepo.findByUuid(schoolId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));
        User user = userService.getCurrentUser();

        if (school.getOrganization() != null && !user.getRole().equals(UserRole.SYS_ADMIN)) {
            if (request.getPlanUuid() != null || request.getSchoolBookUuids() != null || request.getSchoolStatus() != null) {
                throw new ValidationException(MessageKey.ORG_SETTINGS_LOCKED.getKey());
            }
        }

        authToViewEntity.authorizeActionUponSchoolBroadAccess(school);

        if (user.getRole().equals(UserRole.SYS_ADMIN)) {
            if (request.getSchoolStatus() != null) school.setStatus(request.getSchoolStatus());

            if (request.getPlanUuid() != null) {
                SubscriptionPlan plan = subscriptionPlanRepo.findByUuid(request.getPlanUuid())
                        .orElseThrow(() -> new EntityNotFoundException(MessageKey.SUBSCRIPTION_PLAN_NOT_FOUND.getKey()));
                school.setSubscriptionPlan(plan);
            }

            if (request.getExpiresAt() != null) {
                schoolSubscriptionRepo.findBySchool_Uuid(schoolId).ifPresent(sub -> {
                    sub.setExpiresAt(request.getExpiresAt());
                    schoolSubscriptionRepo.save(sub);
                });
            }
        }

        if (request.getLogoId() != null) {
            Attachment logo = attachmentRepo.findByUuid(request.getLogoId())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.ATTACHMENT_NOT_FOUND.getKey()));
            school.setLogo(logo);
        }

        if (request.getRegionUuid() != null) {
            school.setRegion(regionRepository.findByUuid(request.getRegionUuid()).orElseThrow());
        }

        school.setName(request.getName());
        school.setAddress(request.getAddress());
        if (CoreUtils.isPresent(request.getPhone())) school.setContactInfo(request.getPhone());
        if (CoreUtils.isPresent(request.getTelegramLink())) school.setTelegramLink(request.getTelegramLink());

        if (request.getSchoolBookUuids() != null) {
            school.setAllowedBooks(schoolBookRepository.findAllByUuidIn(request.getSchoolBookUuids()));
        }


        schoolRepo.save(school);
        return new ResponseMessage("School updated successfully.");
    }






    private void deleteBooksFromSchools(School school, List<UUID> schoolBookUuids, List<SchoolBook> allowedBooks) {
        List<SchoolBook> schoolBooks = (allowedBooks !=null) ? allowedBooks : new ArrayList<>();
        schoolBookUuids.forEach(b -> {
                    SchoolBook schoolBooke = schoolBookRepository.findByUuid(b).orElseThrow(()
                            -> new EntityNotFoundException(MessageKey.SCHOOL_BOOK_NOT_FOUND.getKey()));
                    schoolBooks.remove(schoolBooke);
                }
        );
        school.setAllowedBooks(schoolBooks);
    }




    @Override
    public ResSchoolInfo getSchoolById(UUID schoolId) {
        School school = schoolRepo.findByUuid(schoolId).orElseThrow();

        authToViewEntity.authorizeActionUponSchoolBroadAccess(school);

        SchoolSubscription sub = schoolSubscriptionRepo.findBySchool_Uuid(school.getUuid()).orElse(null);
        return new ResSchoolInfo(school, (sub != null) ? new ResSchoolSubscription(school, sub) : null);
    }

    @Override
    @Transactional
    public ResponseMessage deleteSchool(UUID schoolId) {
        School school = schoolRepo.findByUuid(schoolId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));
        authToViewEntity.authorizeActionUponSchoolBroadAccess(school);

        school.setStatus(SchoolStatus.DELETED);
        schoolRepo.save(school);

        List<Branch> branches = branchRepository.findAllBySchool_Uuid(schoolId, Pageable.unpaged()).getContent();
        branches.forEach(b -> b.setStatus(BranchStatus.DELETED));
        branchRepository.saveAll(branches);

        groupRepository.findAllByBranch_School_Uuid(schoolId).forEach(group -> {
            groupService.deleteGroup(group.getUuid());
        });

        courseRepo.findAllBySchoolUuid(schoolId).forEach(course -> {
            courseService.deleteCourse(course.getUuid());
        });


        schoolAdminRepo.findAllBySchool_UuidAndUser_Status(schoolId, UserStatus.ACTIVE).forEach(sa -> userService.deleteUser(sa.getUser().getUuid(),null));
        studentRepo.findAllBySchool_UuidAndUser_Status(schoolId, UserStatus.ACTIVE).forEach(s -> userService.deleteUser(s.getUser().getUuid(),null));
        schoolMentorRepo.findAllBySchool_Uuid(schoolId).forEach(sm -> userService.deleteUser(sm.getMentor().getUser().getUuid(),null));
        teacherRepository.findAllBySchool_UuidAndUserStatus(schoolId, UserStatus.ACTIVE, Pageable.unpaged()).forEach(t -> userService.deleteUser(t.getUser().getUuid(),null));

        return new ResponseMessage("School and all associated data moved to recycle bin.");
    }



    @Override
    @Transactional
    public ResponseMessage hardDeleteSchool(UUID schoolId) {
//        School school = schoolRepo.findByUuid(schoolId)
//                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));
//
//        List<SchoolAdmin> schoolAdmins = schoolAdminRepo.findAllBySchool_Uuid(schoolId);
//        List<Moderator> moderators = moderatorRepo.findAllBySchool_Uuid(schoolId);
//        List<Student> students = studentRepo.findAllBySchool_Uuid(schoolId);
//        List<SchoolMentor> schoolMentors = schoolMentorRepo.findAllBySchool_Uuid(schoolId);
////        List<SchoolClass> schoolClasses = schoolClassRepo.findAllBySchoolUuid(schoolId);
//        List<Course> courses = courseRepo.findAllBySchoolUuid(schoolId);
//
//        courseRepo.deleteAll(courses);
//        courseRepo.flush();
//
////        students.forEach(student -> student.getSchoolClasses().clear());
//        studentRepo.saveAllAndFlush(students);
//
////        schoolClassRepo.deleteAll(schoolClasses);
////        schoolClassRepo.flush();
//
//        schoolMentorRepo.deleteAll(schoolMentors);
//        schoolAdminRepo.deleteAll(schoolAdmins);
//        moderatorRepo.deleteAll(moderators);
//        studentRepo.deleteAll(students);
//
//        List<User> usersToDelete = new ArrayList<>();
//        schoolAdmins.forEach(sa -> usersToDelete.add(sa.getUser()));
//        moderators.forEach(m -> usersToDelete.add(m.getUser()));
//        students.forEach(s -> usersToDelete.add(s.getUser()));
//
//        userRepo.deleteAll(usersToDelete);
//
//        schoolRepo.delete(school);

        return new ResponseMessage("not available right now");
    }

    @Override
    @Transactional
    public ResSchoolExamSettings getExamSettings(UUID schoolId) {
        User user = userService.getCurrentUser();

        School school = schoolRepo.findByUuid(schoolId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));

        authToViewEntity.authorizeActionUponSchoolBroadAccess(school);

        SchoolExamSettings settings = schoolExamSettingsRepo.findBySchool_Uuid(school.getUuid())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXAM_SETTINGS_NOT_FOUND.getKey()));

        return new ResSchoolExamSettings(settings);
    }

    @Transactional
    public List<ResBookCoinStats> getBookCoinStats(UUID bookUuid) {
        User user = userService.getCurrentUser();

        Collection<UUID> schoolUuids = userScopeService.getAuthorizedSchoolUuids();

        if (user.getRole().equals(UserRole.SYS_ADMIN) && bookUuid != null) {
            schoolUuids = List.of(bookUuid);
        }

        List<SchoolBook> availableBooks = schoolBookRepository.findAvailableWithFilterMulti(schoolUuids, null);

        if (availableBooks.isEmpty()) return Collections.emptyList();

        List<UUID> bookUuids = availableBooks.stream().map(SchoolBook::getUuid).toList();

        List<Object[]> results = schoolBookRepository.getBookCoinStatsNative(bookUuids);

        return results.stream().map(row -> new ResBookCoinStats(
                UUID.fromString((String) row[0]),
                (String) row[1],
                ((Number) row[2]).longValue(),
                ((Number) row[3]).longValue(),
                ((Number) row[4]).longValue()
        )).toList();
    }

    @Override
    @Transactional
    public ResponseMessage updateExamSettings(UUID schoolId, ReqSchoolExamSettings request) {
        School school = schoolRepo.findByUuid(schoolId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));

        if (school.getOrganization() != null && !userService.getCurrentUser().getRole().equals(UserRole.SYS_ADMIN)) {
            throw new ValidationException(MessageKey.EXAM_SETTINGS_LOCKED.getKey());
        }

        authToViewEntity.authorizeActionUponSchoolBroadAccess(school);

        SchoolExamSettings settings = schoolExamSettingsRepo.findBySchool_Uuid(schoolId)
                .orElseGet(() -> {
                    SchoolExamSettings s = new SchoolExamSettings();
                    s.setSchool(school);
                    return s;
                });

        settings.setSeparateSection(request.isSeparateSection());
        settings.setNoScreenshot(request.isNoScreenshot());
        settings.setAttemptLimit(request.getAttemptLimit());
        settings.setTimeLimit(request.getTimeLimit());
        settings.setSectionTimeLimits(request.getSectionTimeLimits());
        settings.setFreezeScreen(request.isFreezeScreen());
        settings.setFreezeTimer(request.getFreezeTimer());

        schoolExamSettingsRepo.save(settings);
        return new ResponseMessage("Exam policy updated for branch: " + school.getName());
    }

    @Override
    @Transactional
    public ResponseMessage undeleteSchool(UUID schoolId) {
        School school = schoolRepo.findByUuid(schoolId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));

        authToViewEntity.authorizeActionUponSchoolBroadAccess(school);

        if (!school.getStatus().equals(SchoolStatus.DELETED)) {
            return new ResponseMessage("School is already active.");
        }

        school.setStatus(SchoolStatus.ACTIVE);
        schoolRepo.save(school);

        branchRepository.findAllBySchool_Uuid(schoolId, Pageable.unpaged()).getContent()
                .forEach(b -> b.setStatus(BranchStatus.ACTIVE));

        groupRepository.findAllByBranch_School_Uuid(schoolId).forEach(group -> {
            try {
                groupService.unDeleteGroup(group.getUuid());
            } catch (Exception e) {
                Logger.exception("Non-critical: catch for group " + group.getUuid(), e);
            }
        });

        courseRepo.findAllBySchoolUuid(schoolId).forEach(course -> {
            try {
                courseService.undeleteCourse(course.getUuid());
            } catch (Exception e) {
                Logger.exception("Non-critical: catch for course " + course.getUuid(), e);
            }
        });

        List<UUID> usersToRestore = new ArrayList<>();
        schoolAdminRepo.findAllBySchool_UuidAndUser_Status(schoolId, UserStatus.BLOCKED).forEach(sa -> usersToRestore.add(sa.getUser().getUuid()));
        studentRepo.findAllBySchool_UuidAndUser_Status(schoolId, UserStatus.BLOCKED).forEach(s -> usersToRestore.add(s.getUser().getUuid()));
        schoolMentorRepo.findAllBySchool_Uuid(schoolId).forEach(sm -> usersToRestore.add(sm.getMentor().getUser().getUuid()));
        teacherRepository.findAllBySchool_UuidAndUserStatus(schoolId, UserStatus.BLOCKED, Pageable.unpaged()).forEach(t -> usersToRestore.add(t.getUser().getUuid()));

        usersToRestore.forEach(uuid -> {
            try {
                userService.undeleteUser(uuid);
            } catch (Exception e) {
                Logger.exception("Critical error during user restoration for UUID: " + uuid, e);
            }
        });

        return new ResponseMessage("School restoration completed. Any conflicting usernames were skipped and logged.");
    }


    @Override
    public Page<ResMentorsForSchool> getMentorsForSchool(UUID schoolId, Pageable pageable) {
        School school = schoolRepo.findByUuid(schoolId).orElseThrow();
        authToViewEntity.authorizeActionUponSchoolBroadAccess(school);

        return schoolMentorRepo.findAllBySchool_Uuid(schoolId, pageable)
                .map(ResMentorsForSchool::new);
    }


    //TODO kinda useles if doesn't have any futher function
    @Override
    public ResponseMessage updateMentorHours(UUID schoolMentorId, ReqUpdateMentorHours request) {
        SchoolMentor schoolMentor = schoolMentorRepo.findByUuid(schoolMentorId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.MENTOR_ASSIGNMENT_NOT_FOUND.getKey()));
        schoolMentor.setContractHours(request.getContractHours());
        schoolMentorRepo.save(schoolMentor);
        return new ResponseMessage("Mentor contract hours updated");
    }

    @Override
    public ResponseMessage removeMentorFromSchool(UUID schoolMentorId) {
        SchoolMentor schoolMentor = schoolMentorRepo.findByUuid(schoolMentorId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.MENTOR_ASSIGNMENT_NOT_FOUND.getKey()));


        User user = userService.getCurrentUser();
        if(user.getRole().equals(UserRole.SCHOOL_ADMIN)) {
            UUID uuid = userScopeService.getCurrentUserSchoolUuid();
            if(!schoolMentor.getSchool().getUuid().equals(uuid)) {
                throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
            }
        }


        schoolMentorRepo.delete(schoolMentor);
        return new ResponseMessage("Mentor removed from school");
    }
}