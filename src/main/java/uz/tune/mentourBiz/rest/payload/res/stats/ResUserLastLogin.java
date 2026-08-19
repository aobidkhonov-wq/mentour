package uz.tune.mentourBiz.rest.payload.res.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResUserLastLogin {
    private UUID userUuid;
    private String firstName;
    private String lastName;
    private String username;
    private String role;
    private String status;
    private String schoolName;
    private Instant lastActiveAt;
    private Long daysInactive;
    private Instant createdAt;
}
