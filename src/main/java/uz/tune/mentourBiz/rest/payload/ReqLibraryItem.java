package uz.tune.mentourBiz.rest.payload;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import uz.tune.mentourBiz.rest.enums.LibraryItemType;

import java.util.UUID;

@Data
public class ReqLibraryItem {
    private String title;
    private String description;
    private LibraryItemType type;
    private String contentUrl;
    @NotNull(message = "Level uuid is required!")
    private UUID levelUuid;
    private UUID parentUuid;
    private Boolean isGlobal;
    private UUID schoolUuid;
}