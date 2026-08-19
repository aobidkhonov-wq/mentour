package uz.tune.mentourBiz.rest.payload.res.lesson;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ResGroupActiveHomework {
    private UUID groupUuid;
    private String groupName;
    private List<ResActiveHomework> activeHomeworks;
}
