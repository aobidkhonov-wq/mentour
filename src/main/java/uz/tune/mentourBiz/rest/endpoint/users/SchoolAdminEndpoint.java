package uz.tune.mentourBiz.rest.endpoint.users;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.payload.res.user.ResSchoolAdminOne;
import uz.tune.mentourBiz.rest.service.user.SchoolAdminService;

import java.util.UUID;


@RestController
@RequestMapping(BaseURI.API1 + BaseURI.SCHOOL_ADMIN)
@RequiredArgsConstructor
public class SchoolAdminEndpoint {

    private final SchoolAdminService schoolAdminService;

    @GetMapping(BaseURI.ONE)
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).SCHOOL_ADMIN_GET_ONE)")
    public ResponseEntity<ResSchoolAdminOne> getOne(@RequestParam(name = "uuid") UUID uuid) {
        return ResponseEntity.ok(schoolAdminService.getOne(uuid));
    }

    @GetMapping(BaseURI.LIST)
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).SCHOOL_ADMIN_GET_ALL)")
    public ResponseEntity<Page<ResSchoolAdminOne>> getAll(@RequestParam(name="size", defaultValue = "10") int size,
                                                          @RequestParam(name="page", defaultValue = "0") int page,
                                                          @RequestParam(required = false) String schoolUuid) {
        Sort sortByCreatedArtDesc = Sort.by(Sort.Direction.DESC, "schoolId");
        Pageable pageable = PageRequest.of(page, size,sortByCreatedArtDesc);
        return ResponseEntity.ok(schoolAdminService.getAll(pageable, schoolUuid));
    }
}
