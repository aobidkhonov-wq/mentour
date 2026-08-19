package uz.tune.mentourBiz.rest.endpoint;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.payload.res.ResSchoolLibraryStatus;
import uz.tune.mentourBiz.rest.payload.res.ResSchoolReadiness;
import uz.tune.mentourBiz.rest.service.StatisticsService;

import java.util.List;


@RestController
@RequestMapping(BaseURI.API1 + "/statistics")
@RequiredArgsConstructor
public class StatisticsCEndpoint {

    private final StatisticsService statisticsService;
//
//    @GetMapping("/payment")
//    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).READINESS_REPORT_GET)")
//    public ResponseEntity<List<ResSchoolPaymentStatus>> getPaymentReadiness() {
//        return ResponseEntity.ok(statisticsService.getIncompletePaymentSchools());
//    }

    @GetMapping("/school-stats")
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    public ResponseEntity<List<ResSchoolReadiness>> getReadinessReport() {
        return ResponseEntity.ok(statisticsService.getSchoolReadinessReport());
    }

    @GetMapping("/libraries")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).EMPTY_LIBRARY_STATS_GET)")
    public ResponseEntity<List<ResSchoolLibraryStatus>> getEmptyLibraryStats() {
        return ResponseEntity.ok(statisticsService.getSchoolsWithEmptyLibraries());
    }

//    @GetMapping("/shops")
//    @PreAuthorize("hasAuthority('SYS_ADMIN')")
//    public ResponseEntity<List<ResSchoolShopStatus>> getEmptyShopStats() {
//        return ResponseEntity.ok(statisticsService.getSchoolsWithEmptyShops());
//    }



}
