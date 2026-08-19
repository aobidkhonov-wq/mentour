package uz.tune.mentourBiz.rest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.payload.res.ResSchoolLibraryStatus;
import uz.tune.mentourBiz.rest.payload.res.ResSchoolReadiness;
import uz.tune.mentourBiz.rest.payload.res.school.ResSchoolInfo;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final SchoolRepo schoolRepo;
    private final UserScopeService userScopeService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<ResSchoolReadiness> getSchoolReadinessReport() {
        List<UUID> authorizedUuids = userScopeService.getAuthorizedSchoolUuids();

        List<Object[]> results = schoolRepo.findAllSchoolReadinessStats();

        return results.stream()
                .map(row -> {
                    School s = (School) row[0];
                    if (authorizedUuids != null && !authorizedUuids.contains(s.getUuid())) return null;

                    long pkgCount = (Long) row[1];
                    long libCount = (Long) row[2];
                    long shopCount = (Long) row[3];

                    long setupScore = (s.isPaymentActive() ? 10 : 0) + pkgCount + libCount + shopCount;

                    return new ResSchoolReadiness(
                            new ResSchoolInfo(s), s.isPaymentActive(), pkgCount,
                            s.isPaymentActive() ? "Active" : "Inactive", libCount, shopCount, setupScore
                    );
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ResSchoolReadiness::getTotalSetupScore))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ResSchoolLibraryStatus> getSchoolsWithEmptyLibraries() {
        List<UUID> authorizedUuids = userScopeService.getAuthorizedSchoolUuids();
        return schoolRepo.findSchoolsWithEmptyLibrary().stream()
                .filter(s -> authorizedUuids == null || authorizedUuids.contains(s.getUuid()))
                .map(s -> new ResSchoolLibraryStatus(new ResSchoolInfo(s), s.getStudentCount(), "No materials"))
                .toList();
    }
}