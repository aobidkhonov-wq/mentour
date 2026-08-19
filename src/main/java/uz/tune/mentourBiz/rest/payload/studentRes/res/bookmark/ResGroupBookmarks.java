package uz.tune.mentourBiz.rest.payload.studentRes.res.bookmark;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ResGroupBookmarks {
    private UUID studentUuid;
    private String studentFullName;
    private List<ResTeacherBookmark> bookmarks;
}