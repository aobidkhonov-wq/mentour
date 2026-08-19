package uz.tune.mentourBiz.rest.service.exercise;

import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.bookmark.ResCreateBookmark;
import uz.tune.mentourBiz.rest.payload.studentReq.req.bookmark.ReqCreateBookmark;
import uz.tune.mentourBiz.rest.payload.studentRes.res.bookmark.ResGroupBookmarks;

import java.util.List;
import java.util.UUID;

public interface BookmarkService {
    ResCreateBookmark createBookmark(ReqCreateBookmark request);
    ResponseMessage resolveBookmark(UUID bookmarkUuid);
    ResponseMessage deleteBookmark(UUID bookmarkUuid);
    List<ResGroupBookmarks> getBookmarksForGroup(UUID groupId);
}