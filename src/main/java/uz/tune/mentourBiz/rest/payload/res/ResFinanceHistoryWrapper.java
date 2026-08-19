package uz.tune.mentourBiz.rest.payload.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Page;

@Data
@AllArgsConstructor
public class ResFinanceHistoryWrapper {
    private Page<ResFinanceTransaction> historyPage;
    private Long totalRevenueInRange;
}