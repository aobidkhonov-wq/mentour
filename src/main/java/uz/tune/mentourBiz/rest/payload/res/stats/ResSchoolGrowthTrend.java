package uz.tune.mentourBiz.rest.payload.res.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResSchoolGrowthTrend {

    private Instant month;
    private Long newSchools;
    private Long activeSchools;
    private Long frozenSchools;
    private Long deletedSchools;

}
