package uz.tune.mentourBiz.rest.payload;

import lombok.Data;

import java.util.UUID;

@Data
public class ReqUpdateMobileVersion {
    private String iosVersion;
    private String androidVersion;
    private UUID schoolUuid;
}