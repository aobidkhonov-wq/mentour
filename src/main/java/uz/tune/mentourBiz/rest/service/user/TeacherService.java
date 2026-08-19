package uz.tune.mentourBiz.rest.service.user;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uz.tune.mentourBiz.rest.enums.TransactionType;
import uz.tune.mentourBiz.rest.enums.UserStatus;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.shop.ResCoinTransaction;
import uz.tune.mentourBiz.rest.payload.res.user.ResTeacherOne;
import uz.tune.mentourBiz.rest.payload.studentReq.req.ReqAwardCoins;

import java.util.List;
import java.util.UUID;

public interface TeacherService {
    ResponseMessage awardCoinsToStudent(List<ReqAwardCoins> request);
    ResTeacherOne getOne(UUID uuid);
    Page<ResTeacherOne> getAll(Pageable pageable, UserStatus userStatus, UUID schoolId, String fullName);
    Page<ResCoinTransaction> getCoinHistory(Pageable pageable, TransactionType type, String studentName, String givenBy);
}