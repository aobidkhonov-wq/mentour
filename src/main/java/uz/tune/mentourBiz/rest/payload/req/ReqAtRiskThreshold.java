package uz.tune.mentourBiz.rest.payload.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ReqAtRiskThreshold {
    // Optional: target school (for SYS_ADMIN / director). When null the caller's school scope is used.
    private UUID schoolUuid;

    @Min(0)
    @Max(100)
    private Integer attendanceThreshold;

    @Min(0)
    @Max(100)
    private Integer resultsThreshold;
}
