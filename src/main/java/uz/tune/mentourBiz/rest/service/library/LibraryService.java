package uz.tune.mentourBiz.rest.service.library;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uz.tune.mentourBiz.rest.payload.ReqLibraryItem;
import uz.tune.mentourBiz.rest.payload.req.library.ReqGetLibraryItems;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.studentRes.res.lib.ResLibraryItem;

import java.util.UUID;

public interface LibraryService {
    Page<ResLibraryItem> getLibraryItems(ReqGetLibraryItems reqGetLibraryItems, Pageable pageable);
    ResponseMessage upsertItem(ReqLibraryItem req, UUID existingUuid);
//    List<ResBookLibrarySection> getBookMaterials(UUID bookUuid, LibraryItemType type);
    ResponseMessage deleteItem(UUID uuid);
}
