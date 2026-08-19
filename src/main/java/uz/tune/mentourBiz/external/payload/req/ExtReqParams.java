package uz.tune.mentourBiz.external.payload.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtReqParams {

    private String courseId;

    private Integer lessonCount;
}
