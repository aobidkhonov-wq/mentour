package uz.tune.mentourBiz.rest.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.rest.domain.*;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.SchoolBook;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.enums.SchoolStatus;
import uz.tune.mentourBiz.rest.payload.ResOrganization;
import uz.tune.mentourBiz.rest.payload.req.ReqSchoolExamSettings;
import uz.tune.mentourBiz.rest.payload.res.ResAcademicConfig;
import uz.tune.mentourBiz.rest.payload.res.ResSchoolRewardConfig;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.repository.*;
import uz.tune.mentourBiz.rest.repository.school.SchoolBookRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
import uz.tune.mentourBiz.rest.repository.user.UserRepo;
import uz.tune.mentourBiz.rest.service.OrganizationService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final SchoolRepo schoolRepo;
    private final AttachmentRepo attachmentRepo;
    private final UserRepo userRepo;
    private final SchoolDirectorRepo schoolDirectorRepo;
    private final SchoolSubscriptionRepo schoolSubscriptionRepo;
    private final SubscriptionPlanRepo subscriptionPlanRepo;
    private final SchoolAcademicConfigRepo schoolAcademicConfigRepo;
    private final SchoolExamSettingsRepo schoolExamSettingsRepo;
    private final SchoolBookRepository schoolBookRepository;
    private final SchoolRewardConfigRepo schoolRewardConfigRepo;

    @Override
    public Page<ResOrganization> getAll(Pageable pageable) {
        return organizationRepository.findAllByStatusNot(SchoolStatus.DELETED, pageable)
                .map(ResOrganization::new);
    }

    @Override
    public ResOrganization getOne(UUID uuid) {
        return organizationRepository.findByUuid(uuid)
                .map(ResOrganization::new)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.ORG_NOT_FOUND.getKey()));
    }

    @Override
    @Transactional
    public ResponseMessage create(ReqOrganization req) {
        // 1. Initialize Organization
        Organization org = new Organization();
        org.setName(req.getName());
        // Use the status from request or default to ACTIVE
        org.setStatus(req.getStatus() != null ? req.getStatus() : SchoolStatus.ACTIVE);
        org.setExpiresAt(req.getExpiresAt());

        // 2. Handle Logo
        if (req.getLogoId() != null) {
            org.setLogo(attachmentRepo.findByUuid(req.getLogoId()).orElse(null));
        }

        // 3. Handle Subscription Plan
        if (req.getPlanUuid() != null) {
            SubscriptionPlan plan = subscriptionPlanRepo.findByUuid(req.getPlanUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.SUBSCRIPTION_PLAN_NOT_FOUND.getKey()));
            org.setSubscriptionPlan(plan);
        }

        Organization savedOrg = organizationRepository.saveAndFlush(org);

        // 4. Propagate to Schools
        if (req.getSchoolUuids() != null && !req.getSchoolUuids().isEmpty()) {
            List<School> schools = schoolRepo.findAllByUuidIn(req.getSchoolUuids());

            // Resolve Master Books list once
            List<SchoolBook> masterBooks = (req.getBookUuids() != null)
                    ? schoolBookRepository.findAllByUuidIn(req.getBookUuids())
                    : new ArrayList<>();

            for (School sc : schools) {
                // A. Link to Org
                sc.setOrganization(savedOrg);

                // B. Propagate Status (Crucial: makes sure schools aren't 'DELETED' or 'FROZEN' if Org is 'ACTIVE')
                sc.setStatus(savedOrg.getStatus());

                // C. Propagate Plan
                sc.setSubscriptionPlan(savedOrg.getSubscriptionPlan());

                // D. Propagate Books (Curriculum sync)
                sc.setAllowedBooks(new ArrayList<>(masterBooks));

                // E. Propagate Payment Activation
                // If the Organization is created with an active plan, enable billing features
                if (savedOrg.getStatus() == SchoolStatus.ACTIVE) {
                    sc.setPaymentActive(true);
                    if (sc.getPaymentActivatedTime() == null) {
                        sc.setPaymentActivatedTime(java.time.Instant.now());
                    }
                }

                // F. Sync SchoolSubscription table (The Expiry Date)
                SchoolSubscription sub = schoolSubscriptionRepo.findBySchool_Uuid(sc.getUuid())
                        .orElse(new SchoolSubscription());
                sub.setSchool(sc);

                // If Org has no expiry in request, default to 30 days
                java.time.Instant expiry = (savedOrg.getExpiresAt() != null)
                        ? savedOrg.getExpiresAt()
                        : java.time.Instant.now().plus(java.time.Duration.ofDays(30));

                sub.setExpiresAt(expiry);
                schoolSubscriptionRepo.save(sub);

                schoolRepo.save(sc);
            }
        }

        if (req.getDirectorUuid() != null) {
            User user = userRepo.findByUuid(req.getDirectorUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

            SchoolDirector sd = schoolDirectorRepo.findByUser(user).orElse(new SchoolDirector());
            sd.setUser(user);
            sd.setOrganization(savedOrg);
            schoolDirectorRepo.save(sd);
        }

        return new ResponseMessage("Organization created. Settings, billing activation, and curriculum propagated to " +
                (req.getSchoolUuids() != null ? req.getSchoolUuids().size() : 0) + " schools.");
    }

    @Override
    @Transactional
    public ResponseMessage update(UUID uuid, ReqOrganization req) {
        // 1. Fetch the Organization
        Organization org = organizationRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.ORG_NOT_FOUND.getKey()));

        // 2. Update Organization Core Fields
        if (req.getName() != null) org.setName(req.getName());
        if (req.getExpiresAt() != null) org.setExpiresAt(req.getExpiresAt());
        if (req.getStatus() != null) org.setStatus(req.getStatus());

        if (req.getPlanUuid() != null) {
            SubscriptionPlan plan = subscriptionPlanRepo.findByUuid(req.getPlanUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.SUBSCRIPTION_PLAN_NOT_FOUND.getKey()));
            org.setSubscriptionPlan(plan);
        }

        // 3. Resolve Master Books for propagation
        // If bookUuids are provided, those become the organization's current master list.
        // If not provided, we check if the org already has books via existing links (if logic exists)
        // or just skip book propagation.
        List<SchoolBook> masterBooks = null;
        if (req.getBookUuids() != null) {
            masterBooks = schoolBookRepository.findAllByUuidIn(req.getBookUuids());
        }

        // 4. Sync School Relationship (Managing which schools belong to this Org)
        if (req.getSchoolUuids() != null) {
            // Unlink schools currently in this Org
            List<School> currentSchools = org.getSchools();
            if (currentSchools != null) {
                for (School s : currentSchools) {
                    s.setOrganization(null);
                }
                schoolRepo.saveAll(currentSchools);
            }

            //  Link new schools from the request
            List<School> newSchools = schoolRepo.findAllByUuidIn(req.getSchoolUuids());
            for (School s : newSchools) {
                s.setOrganization(org);
            }
            schoolRepo.saveAll(newSchools);

            // Update the local list so the propagation loop (Step 6) sees the changes
            org.setSchools(newSchools);
        }

        // 5. Sync Director
        if (req.getDirectorUuid() != null) {
            List<SchoolDirector> existingDirectors = schoolDirectorRepo.findByOrganizationId(org.getId());
            for (SchoolDirector sd : existingDirectors) {
                sd.setOrganization(null);
            }
            schoolDirectorRepo.saveAll(existingDirectors);

            User directorUser = userRepo.findByUuid(req.getDirectorUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
            SchoolDirector newSd = schoolDirectorRepo.findByUser(directorUser).orElse(new SchoolDirector());
            newSd.setUser(directorUser);
            newSd.setOrganization(org);
            schoolDirectorRepo.save(newSd);
        }

        // 6. TOTAL PROPAGATION: Force Organization settings onto ALL linked schools
        if (org.getSchools() != null && !org.getSchools().isEmpty()) {
            for (School s : org.getSchools()) {

                // A. Propagate Status (e.g., if Org is FROZEN, School becomes FROZEN)
                if (org.getStatus() != null) {
                    s.setStatus(org.getStatus());
                }

                // B. Propagate Subscription Plan
                if (org.getSubscriptionPlan() != null) {
                    s.setSubscriptionPlan(org.getSubscriptionPlan());
                }

                // C. Propagate Expiry Date (Updates the SchoolSubscription record)
                if (org.getExpiresAt() != null) {
                    SchoolSubscription sub = schoolSubscriptionRepo.findBySchool_Uuid(s.getUuid())
                            .orElse(new SchoolSubscription());
                    sub.setSchool(s);
                    sub.setExpiresAt(org.getExpiresAt());
                    schoolSubscriptionRepo.save(sub);
                }

                if (masterBooks != null) {
                    s.setAllowedBooks(new ArrayList<>(masterBooks));
                }

                schoolRepo.save(s);
            }
        }

        organizationRepository.save(org);

        return new ResponseMessage("Organization updated. Configuration and " +
                (masterBooks != null ? masterBooks.size() : 0) + " books propagated to " +
                (org.getSchools() != null ? org.getSchools().size() : 0) + " schools.");
    }

    @Override
    @Transactional
    public ResponseMessage delete(UUID uuid) {
        Organization org = organizationRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.ORG_NOT_FOUND.getKey()));

        if (org.getSchools() != null) {
            org.getSchools().forEach(s -> s.setOrganization(null));
            schoolRepo.saveAll(org.getSchools());
        }

        List<SchoolDirector> directors = schoolDirectorRepo.findByOrganizationId(org.getId());
        if (!directors.isEmpty()) {
            directors.forEach(d -> d.setOrganization(null));
            schoolDirectorRepo.saveAll(directors);
        }

        org.setStatus(SchoolStatus.DELETED);
        organizationRepository.save(org);

        return new ResponseMessage("Organization unlinked and marked as deleted.");
    }

    @Override
    @Transactional
    public ResponseMessage updateOrganizationAcademicConfig(UUID organizationUuid, ResAcademicConfig req) {
        Organization org = organizationRepository.findByUuid(organizationUuid).orElseThrow();
        for (School school : org.getSchools()) {
            SchoolAcademicConfig config = schoolAcademicConfigRepo.findBySchool_Uuid(school.getUuid()).orElse(new SchoolAcademicConfig());
            config.setSchool(school);
            if (req.getMinScoreToPass() != null) config.setMinScoreToPass(req.getMinScoreToPass());
            if (req.getMaxRetries() != null) config.setMaxRetries(req.getMaxRetries());
            if (req.getTeacherMonthlyCoinLimit() != null) config.setTeacherMonthlyCoinLimit(req.getTeacherMonthlyCoinLimit());
            if (req.getPenaltyPerAttempt() != null) config.setPenaltyPerAttempt(req.getPenaltyPerAttempt());
            if (req.getPenaltyEnabled() != null) config.setPenaltyEnabled(req.getPenaltyEnabled());
            if (req.getAttendanceThreshold() != null) config.setAttendanceThreshold(req.getAttendanceThreshold());
            if (req.getResultsThreshold() != null) config.setResultsThreshold(req.getResultsThreshold());
            schoolAcademicConfigRepo.save(config);
        }
        return new ResponseMessage("Config propagated to all schools");
    }

    @Override
    @Transactional
    public ResponseMessage updateOrganizationExamSettings(UUID organizationUuid, ReqSchoolExamSettings req) {
        Organization org = organizationRepository.findByUuid(organizationUuid).orElseThrow();
        for (School school : org.getSchools()) {
            SchoolExamSettings settings = schoolExamSettingsRepo.findBySchool_Uuid(school.getUuid()).orElse(new SchoolExamSettings());
            settings.setSchool(school);
            settings.setNoScreenshot(req.isNoScreenshot());
            settings.setSeparateSection(req.isSeparateSection());
            settings.setNoScreenshot(req.isNoScreenshot());
            settings.setAttemptLimit(req.getAttemptLimit());
            settings.setTimeLimit(req.getTimeLimit());
            settings.setSectionTimeLimits(req.getSectionTimeLimits());
            settings.setFreezeScreen(req.isFreezeScreen());
            settings.setFreezeTimer(req.getFreezeTimer());

            schoolExamSettingsRepo.save(settings);
        }
        return new ResponseMessage("Exam settings propagated to all schools");
    }

    @Override
    @Transactional
    public ResponseMessage updateOrganizationRewardConfig(UUID organizationUuid, ResSchoolRewardConfig req) {
        Organization org = organizationRepository.findByUuid(organizationUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.ORG_NOT_FOUND.getKey()));

        for (School school : org.getSchools()) {
            SchoolRewardConfig config = schoolRewardConfigRepo.findBySchool_Uuid(school.getUuid())
                    .orElse(new SchoolRewardConfig());
            config.setSchool(school);

            // Map fields (reuse logic)
            config.setExerciseAutoEnabled(req.isExerciseAutoEnabled());
            config.setGapFillBase(req.getGapFillBase());
            config.setOrderingBase(req.getOrderingBase());
            config.setMatchingBase(req.getMatchingBase());
            config.setSelectionBase(req.getSelectionBase());
            config.setMultiSelectBase(req.getMultiSelectBase());
            config.setCircleBase(req.getCircleBase());
            config.setTracingBase(req.getTracingBase());
            config.setAudioMultiplierEnabled(req.isAudioMultiplierEnabled());
            config.setAudioMultiplier(req.getAudioMultiplier());
            config.setVocabAutoEnabled(req.isVocabAutoEnabled());
            config.setVocabRewardPerWord(req.getVocabRewardPerWord());

            schoolRewardConfigRepo.save(config);
        }
        return new ResponseMessage("Reward settings propagated to all schools in " + org.getName());
    }
}