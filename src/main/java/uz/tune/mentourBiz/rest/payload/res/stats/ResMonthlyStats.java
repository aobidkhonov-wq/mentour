package uz.tune.mentourBiz.rest.payload.res.stats;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ResMonthlyStats {
    private List<Integer> counts;
    private List<Double> growthRates;
}