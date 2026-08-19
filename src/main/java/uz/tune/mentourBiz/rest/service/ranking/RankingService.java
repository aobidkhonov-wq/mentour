package uz.tune.mentourBiz.rest.service.ranking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uz.tune.mentourBiz.rest.payload.res.ranking.ResStudentForRanking;

import java.util.UUID;

public interface RankingService {

    Page<ResStudentForRanking> getRankingByStudentGroup(Pageable pageable, UUID studentGroupId);
    Page<ResStudentForRanking> getRankingByStudentBranch(Pageable pageable, UUID studentBranchId);
    Page<ResStudentForRanking> getRankingByStudentSchool(Pageable pageable, UUID studentSchoolId);
}
