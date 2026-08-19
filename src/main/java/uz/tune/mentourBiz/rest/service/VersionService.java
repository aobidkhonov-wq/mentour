package uz.tune.mentourBiz.rest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.rest.domain.MobileVersion;
import uz.tune.mentourBiz.rest.domain.Organization;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.enums.AppType;
import uz.tune.mentourBiz.rest.enums.Platform;
import uz.tune.mentourBiz.rest.payload.ReqUpdateMobileVersion;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.repository.OrganizationRepository;
import uz.tune.mentourBiz.rest.repository.VersionRepo;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VersionService {

    private final VersionRepo versionRepo;
    private final SchoolRepo schoolRepo;
    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> checkVersionStatus(String currentVersion, Platform platform, String schoolScope) {
        return checkVersionStatus(currentVersion, platform, schoolScope, AppType.STUDENT);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> checkVersionStatus(String currentVersion, Platform platform, String schoolScope, AppType appType) {
        Optional<MobileVersion> latestOpt = Optional.empty();

        if (CoreUtils.isPresent(schoolScope)) {
            UUID scopeUuid = UUID.fromString(schoolScope);
            latestOpt = versionRepo.findTopByPlatformAndAppTypeAndSchool_UuidOrderByCreatedAtDesc(platform, appType, scopeUuid);
            if (latestOpt.isEmpty()) {
                // scope organizatsiya uuid bo'lishi mumkin
                latestOpt = versionRepo.findTopByPlatformAndAppTypeAndOrganization_UuidOrderByCreatedAtDesc(platform, appType, scopeUuid);
            }
            if (latestOpt.isEmpty()) {
                latestOpt = schoolRepo.findByUuid(scopeUuid)
                        .map(School::getOrganization)
                        .filter(Objects::nonNull)
                        .flatMap(org -> versionRepo.findTopByPlatformAndAppTypeAndOrganization_UuidOrderByCreatedAtDesc(platform, appType, org.getUuid()));
            }
        }

        if (latestOpt.isEmpty()) {
            latestOpt = versionRepo.findTopByPlatformAndAppTypeAndSchoolIsNullAndOrganizationIsNullOrderByCreatedAtDesc(platform, appType);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("app", appType.name());
        if (latestOpt.isEmpty()) {
            response.put("updateRequired", false);
            return response;
        }

        String latestVersion = latestOpt.get().getVersion();
        boolean needsUpdate = CoreUtils.isVersionOutdated(currentVersion, latestVersion);

        response.put("updateRequired", needsUpdate);
        response.put("latestVersion", latestVersion);
        response.put("scope", latestOpt.get().getSchool() != null ? "SCHOOL" : "GLOBAL");

        return response;
    }

    @Transactional
    public ResponseMessage updateAllPlatforms(ReqUpdateMobileVersion req) {
        return updateAllPlatforms(req, AppType.STUDENT);
    }

    @Transactional
    public ResponseMessage updateAllPlatforms(ReqUpdateMobileVersion req, AppType appType) {
        School school = null;
        Organization organization = null;
        if (req.getSchoolUuid() != null) {
            Optional<School> schoolOpt = schoolRepo.findByUuid(req.getSchoolUuid());
            if (schoolOpt.isPresent()) {
                school = schoolOpt.get();
            } else {
                Optional<Organization> byUuid = organizationRepository.findByUuid(req.getSchoolUuid());
                if (byUuid.isPresent()) {
                    organization = byUuid.get();
                }
            }
        }


        if (req.getIosVersion() != null) {
            if (school != null) {
                saveVersion(req.getIosVersion(), Platform.IOS, appType, school);
            }else if (organization != null) {
                saveVersion(req.getIosVersion(), Platform.IOS, appType, organization);
            } else {
                saveVersion(req.getIosVersion(), Platform.IOS, appType);
            }
        }

        if (req.getAndroidVersion() != null) {
            if (school != null) {
                saveVersion(req.getAndroidVersion(), Platform.ANDROID, appType, school);
            } else if (organization != null) {
                saveVersion(req.getAndroidVersion(), Platform.ANDROID, appType, organization);
            } else {
                saveVersion(req.getAndroidVersion(), Platform.ANDROID, appType);
            }
        }

        return new ResponseMessage(appType.name() + " app versions updated successfully" + (school != null ? " for " + school.getName() : (organization != null ? " for " + organization.getName() : " globally")));
    }

    private void saveVersion(String version, Platform platform, AppType appType, School school) {
        MobileVersion mv = new MobileVersion();
        mv.setVersion(version);
        mv.setPlatform(platform);
        mv.setAppType(appType);
        mv.setSchool(school);
        versionRepo.save(mv);
    }
    private void saveVersion(String version, Platform platform, AppType appType, Organization organization) {
        MobileVersion mv = new MobileVersion();
        mv.setVersion(version);
        mv.setPlatform(platform);
        mv.setAppType(appType);
        mv.setOrganization(organization);
        versionRepo.save(mv);
    }
    private void saveVersion(String version, Platform platform, AppType appType) {
        MobileVersion mv = new MobileVersion();
        mv.setVersion(version);
        mv.setPlatform(platform);
        mv.setAppType(appType);
        versionRepo.save(mv);
    }
}