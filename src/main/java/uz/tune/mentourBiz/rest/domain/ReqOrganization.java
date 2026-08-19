package uz.tune.mentourBiz.rest.domain;

import lombok.Data;
import uz.tune.mentourBiz.rest.enums.SchoolStatus;

import java.util.List;
import java.util.UUID;

@Data
public class ReqOrganization {
    private String name;
    private UUID logoId;
    private List<UUID> schoolUuids;
    private UUID directorUuid;
    private SchoolStatus status;
    private java.time.Instant expiresAt;
    private UUID planUuid;
    private List<UUID> bookUuids;
}
