package uz.tune.mentourBiz.rest.payload.req.group;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ReqGroupUpdate {
    private String name;
    private UUID teacherId;
    private UUID levleUuid;
    private List<String> lessonDays;

}