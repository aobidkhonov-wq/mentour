package uz.tune.mentourBiz.rest.endpoint.statistics;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.enums.StudentSummaryMetric;
import uz.tune.mentourBiz.rest.payload.ResTeacherRank;
import uz.tune.mentourBiz.rest.payload.res.ResDirectorOverallStats;
import uz.tune.mentourBiz.rest.payload.res.ResStudentDashboardSummary;
import uz.tune.mentourBiz.rest.payload.res.stats.ResMonthlyStats;
import uz.tune.mentourBiz.rest.payload.res.user.ResStudentCount;
import uz.tune.mentourBiz.rest.payload.res.user.ResStudentList;
import uz.tune.mentourBiz.rest.service.exercise.StudentLessonService;
import uz.tune.mentourBiz.rest.service.statistics.StatisticSsService;
import uz.tune.mentourBiz.stats.ResGrowthMetrics;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + "/stats")
@RequiredArgsConstructor
public class StatisticsEndpoint {

    private final StatisticSsService statisticsService;
    private final StudentLessonService studentLessonService;

    @GetMapping("/students")
    @Hidden
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).STUDENT_STATS_GET)")
    public ResponseEntity<ResMonthlyStats> getStudentStats(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) UUID schoolUuid) {

        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(statisticsService.getStudentInflowStats(targetYear, schoolUuid));
    }

//    @GetMapping("/director/dashboard")
//    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).GROWTH_GET)")
//    public ResponseEntity<Map<String, Object>> getDirectorDashboard() {
//        return ResponseEntity.ok(statisticsService.getDirectorAggregateDashboard());
//    }

//    @GetMapping("/organization/growth")
//    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'SCHOOL_DIRECTOR')")
//    public ResponseEntity<ResGrowthMetrics> getOrgGrowth(@RequestParam(required = false) UUID organizationUuid) {
//        return ResponseEntity.ok(statisticsService.getOrganizationGrowthMetrics(organizationUuid));
//    }

    @GetMapping("/growth")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'SCHOOL_ADMIN', 'SCHOOL_DIRECTOR', 'TEACHER')")
    public ResponseEntity<ResGrowthMetrics> getGrowth(
            @RequestParam(required = false) UUID schoolUuid) {
        return ResponseEntity.ok(statisticsService.getGrowthMetrics(schoolUuid));
    }

    @GetMapping("/students/summary")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'SCHOOL_DIRECTOR', 'SCHOOL_ADMIN','TEACHER')")
    public ResponseEntity<ResStudentDashboardSummary> getStudentSummary(
            @RequestParam(required = false) UUID schoolUuid) {
        return ResponseEntity.ok(studentLessonService.getStudentSummary(schoolUuid));
    }


    @GetMapping("/organization/growth")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).GROWTH_ORG_GET)")
    public ResponseEntity<ResDirectorOverallStats> getGrowthOrg(
            @RequestParam(required = false) UUID organizationUuid, @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(statisticsService.getDirectorDetailedStats(organizationUuid, activeOnly));
    }

    @GetMapping("/top-teachers")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).TOP_TEACHERS_GET)")
    public ResponseEntity<List<ResTeacherRank>> getTopTeachers(
            @RequestParam(required = false) UUID schoolUuid,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(statisticsService.getTopTeachers(schoolUuid, pageable));
    }

    @GetMapping("/students/summary/details")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'SCHOOL_DIRECTOR', 'SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<Page<ResStudentList>> getSummaryDetails(
            @RequestParam(required = false) UUID schoolUuid,
            @RequestParam StudentSummaryMetric metric,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(studentLessonService.getStudentSummaryDetails(schoolUuid, metric, pageable));
    }

    @GetMapping("/level/students/count")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'SCHOOL_DIRECTOR', 'SCHOOL_ADMIN')")
    public ResponseEntity<List<ResStudentCount>> getStudentsCount(
            @RequestParam(required = false) UUID schoolUuid) {
        return ResponseEntity.ok(studentLessonService.getStudentsCount(schoolUuid));
    }
}