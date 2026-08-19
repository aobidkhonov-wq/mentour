package uz.tune.mentourBiz.rest.payload.req.group;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ReqGroupCreate {
    private String name;
    private UUID schoolId;
    private UUID teacherId;
    private UUID levelId;
    private List<String> lessonDays;
}