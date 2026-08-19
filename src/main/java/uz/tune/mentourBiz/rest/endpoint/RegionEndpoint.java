package uz.tune.mentourBiz.rest.endpoint;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.domain.Region;
import uz.tune.mentourBiz.rest.payload.req.ReqRegionUpdate;
import uz.tune.mentourBiz.rest.payload.res.ResRegion;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.repository.RegionRepository;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + "/regions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
public class RegionEndpoint {

    private final RegionRepository repo;

    @GetMapping
    public ResponseEntity<List<ResRegion>> getAll() {
        return ResponseEntity.ok(repo.findAll().stream().map(ResRegion::new).toList());
    }

    @PostMapping
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).CREATE)")
    public ResponseEntity<ResponseMessage> create(@RequestBody Region region) {
        region.setUuid(UUID.randomUUID());
        repo.save(region);
        return ResponseEntity.ok(new ResponseMessage("Region created"));
    }

    @PatchMapping("/{uuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).UPDATE)")
    @Transactional
    public ResponseEntity<ResponseMessage> update(@PathVariable UUID uuid, @RequestBody ReqRegionUpdate req) {
        Region region = repo.findByUuid(uuid)
                .orElseThrow(() -> new uz.tune.mentourBiz.exception.EntityNotFoundException(MessageKey.REGION_NOT_FOUND.getKey()));

        if (req.getName() != null) region.setName(req.getName());
        if (req.getCountry() != null) region.setCountry(req.getCountry());
        if (req.getPhoneCode() != null) region.setPhoneCode(req.getPhoneCode());
        if (req.getCurrency() != null) region.setCurrency(req.getCurrency());
        if (req.getLang() != null) region.setLang(req.getLang());

        repo.save(region);
        return ResponseEntity.ok(new ResponseMessage("Region updated successfully"));
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).DELETE)")
    public ResponseEntity<ResponseMessage> delete(@PathVariable UUID uuid) {
        Region region = repo.findByUuid(uuid).orElseThrow();
        repo.delete(region);
        return ResponseEntity.ok(new ResponseMessage("Region deleted"));
    }
}
