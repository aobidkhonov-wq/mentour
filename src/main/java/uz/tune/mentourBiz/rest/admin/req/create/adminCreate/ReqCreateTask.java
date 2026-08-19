package uz.tune.mentourBiz.rest.admin.req.create.adminCreate;

import lombok.Data;
import uz.tune.mentourBiz.rest.enums.LessonSectionType;

import java.util.UUID;

@Data
public class ReqCreateTask {
    private UUID unitUuid;
    private String title;
    private String topic;
    private Integer sortOrder;
    private LessonSectionType sectionType;
}