package uz.tune.mentourBiz.rest.payload.req.attempt;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ReqResetAttempt {
    @NotNull
    private UUID studentUuid;

    @NotNull
    private UUID taskUuid;

    private Integer extraAttempts;
}
