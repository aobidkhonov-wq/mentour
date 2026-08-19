package uz.tune.mentourBiz.rest.payload.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import uz.tune.mentourBiz.rest.payload.studentRes.res.lib.ResLibraryItem;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class ResBookLibrarySection {
    private UUID unitUuid;
    private String unitTitle;
    private Integer sortOrder;
    private List<ResLibraryItem> materials;
}