package uz.tune.mentourBiz.rest.payload.req;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReqAiBatchAnalysis {
    private String studentUiLanguage;
    private List<ReqAiBatchItem> items;
}
