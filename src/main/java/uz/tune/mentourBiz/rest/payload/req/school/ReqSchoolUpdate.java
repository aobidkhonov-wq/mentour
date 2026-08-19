package uz.tune.mentourBiz.rest.payload.req.school;

import lombok.Data;
import uz.tune.mentourBiz.rest.enums.SchoolStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
public class ReqSchoolUpdate {
    private String name;
    private String address;
    private String phone;
    private String telegramLink;
    private UUID logoId;
    private List<UUID> schoolBookUuids;
    private SchoolStatus schoolStatus;
    private UUID planUuid;
    private UUID regionUuid;
    private Instant expiresAt;
    private Integer utcOffset;
}