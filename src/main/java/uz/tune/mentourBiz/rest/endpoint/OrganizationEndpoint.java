package uz.tune.mentourBiz.rest.endpoint;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.domain.ReqOrganization;
import uz.tune.mentourBiz.rest.payload.ResOrganization;
import uz.tune.mentourBiz.rest.payload.req.ReqSchoolExamSettings;
import uz.tune.mentourBiz.rest.payload.res.ResAcademicConfig;
import uz.tune.mentourBiz.rest.payload.res.ResSchoolRewardConfig;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.service.OrganizationService;

import java.util.UUID;


@RestController
@RequestMapping(BaseURI.API1 + "/admin/organizations")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SYS_ADMIN')")
public class OrganizationEndpoint {

    private final OrganizationService organizationService;

    @GetMapping
    public ResponseEntity<Page<ResOrganization>> getAll(Pageable pageable) {
        return ResponseEntity.ok(organizationService.getAll(pageable));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ResOrganization> getOne(@PathVariable UUID uuid) {
        return ResponseEntity.ok(organizationService.getOne(uuid));
    }

    @PostMapping
    public ResponseEntity<ResponseMessage> create(@RequestBody ReqOrganization req) {
        return ResponseEntity.ok(organizationService.create(req));
    }

    @PatchMapping("/{uuid}")
    public ResponseEntity<ResponseMessage> update(@PathVariable UUID uuid, @RequestBody ReqOrganization req) {
        return ResponseEntity.ok(organizationService.update(uuid, req));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<ResponseMessage> delete(@PathVariable UUID uuid) {
        return ResponseEntity.ok(organizationService.delete(uuid));
    }

    @PatchMapping("/{uuid}/configs/academic")
    public ResponseEntity<ResponseMessage> updateAcademicConfigs(@PathVariable UUID uuid, @RequestBody ResAcademicConfig req) {
        return ResponseEntity.ok(organizationService.updateOrganizationAcademicConfig(uuid, req));
    }

    @PatchMapping("/{uuid}/configs/exams")
    public ResponseEntity<ResponseMessage> updateExamConfigs(@PathVariable UUID uuid, @RequestBody ReqSchoolExamSettings req) {
        return ResponseEntity.ok(organizationService.updateOrganizationExamSettings(uuid, req));
    }

    @PatchMapping("/{uuid}/configs/rewards")
    public ResponseEntity<ResponseMessage> updateRewardConfigs(@PathVariable UUID uuid, @RequestBody ResSchoolRewardConfig req) {
        return ResponseEntity.ok(organizationService.updateOrganizationRewardConfig(uuid, req));
    }
}
