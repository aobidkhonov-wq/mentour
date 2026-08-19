package uz.tune.mentourBiz.rest.endpoint.ranking;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.payload.res.ranking.ResStudentForRanking;
import uz.tune.mentourBiz.rest.service.ranking.RankingService;

import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + BaseURI.RANKING)
@RequiredArgsConstructor
public class RankingEndpoint {

    private final RankingService rankingService;;
    //for students

    @GetMapping(BaseURI.GROUPS)
    public ResponseEntity<Page<ResStudentForRanking>> getRankingByStudentGroup(
            @PageableDefault(sort = "student.lifeTimeCoinBalance", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(name = "groupUuid", required = false) UUID groupUuid) {
        return ResponseEntity.ok(rankingService.getRankingByStudentGroup(pageable, groupUuid));
    }

    // lvl boyicha ranking

    @GetMapping(BaseURI.BRANCH)
    public ResponseEntity<Page<ResStudentForRanking>> getRankingByStudentBranch(
            @PageableDefault(sort = "student.lifeTimeCoinBalance", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(name = "branchUuid", required = false) UUID branchUuid) {
        return ResponseEntity.ok(rankingService.getRankingByStudentBranch(pageable, branchUuid));
    }

    @GetMapping(BaseURI.SCHOOLS)
    public ResponseEntity<Page<ResStudentForRanking>> getRankingByStudentSchool(
            @PageableDefault(sort = "student.lifeTimeCoinBalance", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(name = "schoolUuid", required = false) UUID schoolUuid) {
        return ResponseEntity.ok(rankingService.getRankingByStudentSchool(pageable, schoolUuid));
    }


}
