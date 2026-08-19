package uz.tune.mentourBiz.rest.payload.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResBookCoinStats {
    private UUID bookUuid;
    private String bookName;
    private Long exerciseCoins;
    private Long vocabularyCoins;
    private Long totalCoins;
}