package uz.tune.mentourBiz.rest.admin.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import uz.tune.mentourBiz.rest.enums.LessonSectionType;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ResContentHeader {
    private UUID uuid;
    private String title;
    private String topic;
    private Integer sortOrder;
    private String status;
    private LessonSectionType sectionType;
}