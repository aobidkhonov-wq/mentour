package uz.tune.mentourBiz.rest.payload.req.school;

import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
public class ReqSchoolCreate {
    private String name;
    private String address;
    private String phone;
    private String telegramLink;
    private UUID logoId;
    private List<UUID> schoolBookUuids;
    private UUID planUuid;
    private Instant expiresAt;
    private UUID regionUuid;
}