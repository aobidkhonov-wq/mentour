package uz.tune.mentourBiz.rest.payload.req.course;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ReqCourse {
    private String name;
    private String description;
    private List<UUID> bookIds;
    private UUID groupId;
    private UUID packageUuid;
    // Optional; when null the course starts today.
    private Instant courseStartDate;
    private Integer courseDuration;
}