package uz.tune.mentourBiz.rest.payload.studentRes.res.lib;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.libriary.LibraryItem;
import uz.tune.mentourBiz.rest.enums.LibraryItemType;
import uz.tune.mentourBiz.rest.payload.res.level.ResLevel;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
public class ResLibraryItem {
    private UUID itemUuid;
    private String title;
    private String description;
    private LibraryItemType type;
    private String contentUrl;
    private ResLevel level;
    private boolean isGlobal;
    private List<ResLibraryItem> children;
    private UUID schoolUuid;

    public ResLibraryItem(LibraryItem item) {
        this.itemUuid = item.getUuid();
        this.title = item.getTitle();
        this.type = item.getType();
        this.description = item.getDescription();
        this.isGlobal = item.isGlobal();
        this.schoolUuid = (CoreUtils.isPresent(item.getSchool()) ? item.getSchool().getUuid() : null);
        this.contentUrl = CoreUtils.getBaseFileUrl() + item.getContentUrl();
        if (item.getLevel() != null) {
            this.level = new ResLevel(item.getLevel());
        }

        if (item.getChildren() != null && !item.getChildren().isEmpty()) {
            this.children = item.getChildren().stream()
                    .map(ResLibraryItem::new)
                    .collect(Collectors.toList());
        }
    }
}