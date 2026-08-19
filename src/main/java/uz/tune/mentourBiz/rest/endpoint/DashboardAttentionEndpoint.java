package uz.tune.mentourBiz.rest.endpoint;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.enums.AttentionType;
import uz.tune.mentourBiz.rest.service.AttentionRequiredService;

import java.util.Map;

@RestController
@RequestMapping(BaseURI.API1 + "/dashboard/attention")
@RequiredArgsConstructor
public class DashboardAttentionEndpoint {

    private final AttentionRequiredService attentionService;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Long>> getSummary() {
        return ResponseEntity.ok(attentionService.getSummaryCounts());
    }

    @GetMapping("/details")
    public ResponseEntity<Page<?>> getDetails(
            @RequestParam AttentionType type,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(attentionService.getAttentionDetails(type, pageable));
    }
}
