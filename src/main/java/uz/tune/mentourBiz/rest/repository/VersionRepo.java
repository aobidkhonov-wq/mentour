package uz.tune.mentourBiz.rest.repository;

import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.MobileVersion;
import uz.tune.mentourBiz.rest.domain.Organization;
import uz.tune.mentourBiz.rest.enums.AppType;
import uz.tune.mentourBiz.rest.enums.Platform;

import java.util.Optional;
import java.util.UUID;

public interface VersionRepo extends BaseRepository<MobileVersion> {
    Optional<MobileVersion> findTopByPlatformOrderByCreatedAtDesc(Platform platform);

    Optional<MobileVersion> findTopByPlatformAndSchool_UuidOrderByCreatedAtDesc(Platform platform, UUID schoolUuid);
    Optional<MobileVersion> findTopByPlatformAndOrganization_UuidOrderByCreatedAtDesc(Platform platform, UUID organizationUuid);
    Optional<MobileVersion> findTopByPlatformAndSchoolIsNullOrderByCreatedAtDesc(Platform platform);

    Optional<MobileVersion> findTopByPlatformAndOrganizationIsNullOrderByCreatedAtDesc(Platform platform);

    Optional<MobileVersion> findTopByPlatformAndSchoolIsNullAndOrganizationIsNullOrderByCreatedAtDesc(Platform platform);

    Optional<MobileVersion> findTopByPlatformAndAppTypeAndSchool_UuidOrderByCreatedAtDesc(Platform platform, AppType appType, UUID schoolUuid);

    Optional<MobileVersion> findTopByPlatformAndAppTypeAndOrganization_UuidOrderByCreatedAtDesc(Platform platform, AppType appType, UUID organizationUuid);

    Optional<MobileVersion> findTopByPlatformAndAppTypeAndSchoolIsNullAndOrganizationIsNullOrderByCreatedAtDesc(Platform platform, AppType appType);
}
