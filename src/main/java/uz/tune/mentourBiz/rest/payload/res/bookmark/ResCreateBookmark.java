package uz.tune.mentourBiz.rest.payload.res.bookmark;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class ResCreateBookmark {
    private UUID bookmarkUuid;
    private String message;
}
