package uz.tune.mentourBiz.external.payload.res;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExtResBase<T> {
    @JsonProperty("data")
    private T data;

    @JsonProperty("code")
    private Integer code;

    @JsonProperty("errorMessage")
    private String errorMessage;

    @JsonProperty("timestamp")
    private long timestamp;
}
