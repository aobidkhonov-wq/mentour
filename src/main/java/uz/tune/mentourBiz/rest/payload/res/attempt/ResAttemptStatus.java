package uz.tune.mentourBiz.rest.payload.res.attempt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResAttemptStatus {
    private UUID taskUuid;
    private int schoolLimit;
    private int extraAttempts;
    private int attemptsUsed;
    private int remaining;
    private boolean canAttempt;
}
