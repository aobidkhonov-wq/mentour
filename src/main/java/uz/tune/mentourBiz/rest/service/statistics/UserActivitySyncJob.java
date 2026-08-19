package uz.tune.mentourBiz.rest.service.statistics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivitySyncJob {

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Tashkent")
    public void sync() {
        int rows = jdbcTemplate.update("""
                INSERT INTO user_activity (user_id, date, last_seen_at, role)
                SELECT id, CURRENT_DATE, last_active_at, role::VARCHAR
                FROM users
                WHERE last_active_at >= NOW() - INTERVAL '15 minutes'
                  AND last_active_at IS NOT NULL
                ON CONFLICT (user_id, date)
                DO UPDATE SET last_seen_at = EXCLUDED.last_seen_at
                """);
        log.info("user_activity sync: {} rows upserted", rows);
    }
}
