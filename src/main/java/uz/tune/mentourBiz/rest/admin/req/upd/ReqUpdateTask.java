package uz.tune.mentourBiz.rest.admin.req.upd;

import lombok.Data;
import uz.tune.mentourBiz.rest.enums.ExerciseTaskStatus;

@Data
public class ReqUpdateTask {
    private String title;
    private String topic;
    private Integer sortOrder;
    private ExerciseTaskStatus status;
}
