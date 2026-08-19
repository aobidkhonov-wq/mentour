package uz.tune.mentourBiz.rest.payload.studentReq.req.bookmark;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class ReqCreateBookmark {
    private UUID questionUuid;
    private String comment;
}