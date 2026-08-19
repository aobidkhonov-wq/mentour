package uz.tune.mentourBiz.rest.payload.res.user;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
@Getter
@Setter
public class ResStudentCount {
    private String level;
    private Long count;
}
