package uz.tune.mentourBiz.rest.payload.req.library;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.LibraryItemType;

import java.util.UUID;

@Getter
@Setter
public class ReqGetLibraryItems {
    @NotNull
    private LibraryItemType itemType;
    private UUID levelId;
    private UUID schoolUuid;
    private String title;
}
