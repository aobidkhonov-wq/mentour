package uz.tune.mentourBiz.rest.payload.studentReq.req;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class ReqAwardCoins {
    private UUID studentUuid;
    private Integer amount;
    private String comment;
}