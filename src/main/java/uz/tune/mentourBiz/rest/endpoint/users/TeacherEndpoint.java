package uz.tune.mentourBiz.rest.endpoint.users;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.enums.TransactionType;
import uz.tune.mentourBiz.rest.enums.UserStatus;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.shop.ResCoinTransaction;
import uz.tune.mentourBiz.rest.payload.res.user.ResTeacherOne;
import uz.tune.mentourBiz.rest.payload.studentReq.req.ReqAwardCoins;
import uz.tune.mentourBiz.rest.service.user.TeacherService;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping(BaseURI.API1 + BaseURI.TEACHER)
@RequiredArgsConstructor
public class TeacherEndpoint {

    private final TeacherService teacherService;

    @PostMapping("/award-coins")
    public ResponseEntity<ResponseMessage> awardCoins(@RequestBody List<ReqAwardCoins> request) {
        return ResponseEntity.ok(teacherService.awardCoinsToStudent(request));
    }


    @GetMapping(BaseURI.ONE)
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).ONE_GET)")
    public ResponseEntity<ResTeacherOne> getOne(@RequestParam(name = "uuid") UUID uuid) {
        return ResponseEntity.ok(teacherService.getOne(uuid));
    }

    @GetMapping(BaseURI.LIST)
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).ALL_GET)")
    public ResponseEntity<Page<ResTeacherOne>> getAll(
            @RequestParam(name="size", defaultValue = "10") int size,
            @RequestParam(name="page", defaultValue = "0") int page,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) UUID schoolId,
            @RequestParam(required = false) String fullName
    ) {
        Sort sortByCreatedArtDesc = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page, size, sortByCreatedArtDesc);

        return ResponseEntity.ok(teacherService.getAll(pageable, status, schoolId, fullName));
    }

    @GetMapping("/coins/history")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).COIN_HISTORY_GET)")
    public ResponseEntity<Page<ResCoinTransaction>> getCoinHistory(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) String givenBy,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(teacherService.getCoinHistory(pageable, type, studentName, givenBy));
    }



}