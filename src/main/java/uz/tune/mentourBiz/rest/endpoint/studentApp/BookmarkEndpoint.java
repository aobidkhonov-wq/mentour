package uz.tune.mentourBiz.rest.endpoint.studentApp;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.bookmark.ResCreateBookmark;
import uz.tune.mentourBiz.rest.payload.studentReq.req.bookmark.ReqCreateBookmark;
import uz.tune.mentourBiz.rest.payload.studentRes.res.bookmark.ResGroupBookmarks;
import uz.tune.mentourBiz.rest.service.exercise.BookmarkService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + BaseURI.BOOKMARKS)
@RequiredArgsConstructor
public class BookmarkEndpoint {

    private final BookmarkService bookmarkService;

    @PostMapping
    public ResponseEntity<ResCreateBookmark> createBookmark(@RequestBody ReqCreateBookmark request) {
        return new ResponseEntity<>(bookmarkService.createBookmark(request), HttpStatus.CREATED);
    }

    @PatchMapping("/{bookmarkUuid}/resolve")
    public ResponseEntity<ResponseMessage> resolveBookmark(@PathVariable UUID bookmarkUuid) {
        return ResponseEntity.ok(bookmarkService.resolveBookmark(bookmarkUuid));
    }

    @DeleteMapping("/{bookmarkUuid}")
    public ResponseEntity<Void> deleteBookmark(@PathVariable UUID bookmarkUuid) {
        bookmarkService.deleteBookmark(bookmarkUuid);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/groups/{groupUuid}")
    public ResponseEntity<List<ResGroupBookmarks>> getBookmarksForGroup(@PathVariable UUID groupUuid){
        return ResponseEntity.ok(bookmarkService.getBookmarksForGroup(groupUuid));
    }

}