package uz.tune.mentourBiz.rest.service.statistics;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MaterializedViewRefreshScheduler {

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(cron = "0 45 14 * * *", zone = "Asia/Tashkent")
    public void refreshSchoolLastScheduledLesson() {
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY school_last_scheduled_lesson");
    }

}
