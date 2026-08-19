package uz.tune.mentourBiz.rest.endpoint.studentApp;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.payload.ReqLibraryItem;
import uz.tune.mentourBiz.rest.payload.req.library.ReqGetLibraryItems;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.studentRes.res.lib.ResLibraryItem;
import uz.tune.mentourBiz.rest.service.library.LibraryService;

import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + "/library")
@RequiredArgsConstructor
public class LibraryEndpoint {

    private final LibraryService libraryService;


    @PostMapping("/admin/upsert")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).UPSERT)")
    public ResponseEntity<ResponseMessage> upsert(
            @RequestBody ReqLibraryItem req,
            @RequestParam(required = false) UUID uuid) {
        return ResponseEntity.ok(libraryService.upsertItem(req, uuid));
    }

//    @GetMapping("/books/{bookUuid}/materials")
//    public ResponseEntity<List<ResBookLibrarySection>> getBookMaterials(
//            @PathVariable UUID bookUuid,
//            @RequestParam LibraryItemType type) {
//        return ResponseEntity.ok(libraryService.getBookMaterials(bookUuid, type));
//    }

    @GetMapping("/view")
    public ResponseEntity<Page<ResLibraryItem>> getItems(ReqGetLibraryItems req, Pageable pageable) {
        return ResponseEntity.ok(libraryService.getLibraryItems(req, pageable));
    }

    @DeleteMapping("/admin/{uuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).DELETE)")
    public ResponseEntity<ResponseMessage> delete(@PathVariable UUID uuid) {
        return ResponseEntity.ok(libraryService.deleteItem(uuid));
    }

}