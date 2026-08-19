package uz.tune.mentourBiz.rest.payload.req.school;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqClassUpdate {
    private String name;
    private Integer studentCount;
}