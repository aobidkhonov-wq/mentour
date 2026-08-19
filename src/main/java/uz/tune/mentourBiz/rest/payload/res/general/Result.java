package uz.tune.mentourBiz.rest.payload.res.general;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.config.GlobalVar;
import uz.tune.mentourBiz.rest.enums.ActionTypeEnum;

/**
 *  
 * Date: 03.10.2022
 * Time: 18:03
 */
@Getter
@Setter
@NoArgsConstructor
public class Result<T> {
    @JsonProperty("code")
    private Integer code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("audit")
    private String audit;

    @JsonProperty("data")
    private T data;

    @JsonProperty("actionType")
    private ActionTypeEnum actionType;

    public Result(Integer code, String message, T data, ActionTypeEnum actionType) {
        this.code = code;
        this.message = message;
        this.audit = GlobalVar.getRequestId();
        this.data = data;
        this.actionType = actionType;
    }
}
