package uz.tune.mentourBiz.rest.endpoint;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.AttendanceNotificationScheduler;

@RestController
@RequestMapping(BaseURI.API1 + BaseURI.SCHEDULER)
@RequiredArgsConstructor
@PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).PROCESS_NOTIFICATIONS)")
public class SchedulerEndpoint {
    private final AttendanceNotificationScheduler attendanceNotificationScheduler;

    @PostMapping("/notification")
    public void processNotifications() {
        attendanceNotificationScheduler.processNotifications();
    }

    @PostMapping("/coins")
    public void coins() {
        attendanceNotificationScheduler.resetMonthlyTeacherAllowances();
    }

    // Manual trigger for the old PaymentPackage monthly charge removed to stop double-charging;
    // FIXED_MONTHLY billing now runs automatically via processMonthlyRenewals().

    @PostMapping("/afterLesson")
    public void afterLesson() {
        attendanceNotificationScheduler.processImmediateAttendance();
    }

    @PostMapping("/weekly")
    public void processWeeklyReports() {
        attendanceNotificationScheduler.processWeeklyReports();
    }


}
