package uz.tune.mentourBiz.rest.payload.req.attempt;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ReqSetAttemptLimit {
    private UUID schoolUuid;

    @NotNull
    @Min(1)
    private Integer limit;
}
