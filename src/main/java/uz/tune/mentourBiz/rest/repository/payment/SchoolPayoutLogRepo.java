package uz.tune.mentourBiz.rest.repository.payment;

import org.springframework.stereotype.Repository;
import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.transaction.payment.SchoolPayoutLog;

@Repository
public interface SchoolPayoutLogRepo extends BaseRepository<SchoolPayoutLog> {
}