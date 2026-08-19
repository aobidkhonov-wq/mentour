package uz.tune.mentourBiz.rest.payload;

import lombok.Data;

import java.util.UUID;

@Data
public class ReqUpsertListeningTask {
    private UUID taskUuid;
    private UUID unitUuid;
    private String title;
    private String topic;
    private Integer sortOrder;
}
