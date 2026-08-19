package uz.tune.mentourBiz.rest.payload.res.general;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import uz.tune.mentourBiz.rest.enums.ActionTypeEnum;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneralResponse<T> {

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("result")
    private Result<T> result;

    public GeneralResponse(Result<T> result, boolean success) {
        this.result = result;
        this.success = success;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> ResponseEntity<GeneralResponse<T>> success(T data, Integer code, String message) {
        return ResponseEntity.ok(
                new GeneralResponse<>(new Result<>(code, message, data, ActionTypeEnum.SUCCESS), true)
        );
    }

    public static <T> ResponseEntity<GeneralResponse<T>> success(T data, Integer code, String message, ActionTypeEnum actionType) {
        return ResponseEntity.ok(
                new GeneralResponse<>(new Result<>(code, message, data, actionType), true)
        );
    }

    public static <T> ResponseEntity<GeneralResponse<T>> success(T data) {
        return ResponseEntity.ok(
                new GeneralResponse<>(new Result<>(20000, "SUCCESS", data, ActionTypeEnum.SUCCESS), true)
        );
    }

    public static <T> ResponseEntity<GeneralResponse<T>> success(T data, Integer code) {
        return ResponseEntity.ok(
                new GeneralResponse<>(new Result<>(code, null, data, ActionTypeEnum.SUCCESS), true)
        );
    }

    public static <T> ResponseEntity<GeneralResponse<T>> error(T data, Integer code, String message) {
        return ResponseEntity.ok(
                new GeneralResponse<>(new Result<>(code, message, data, ActionTypeEnum.ERROR), false)
        );
    }

    public static <T> ResponseEntity<GeneralResponse<T>> error(String message, ActionTypeEnum actionType) {
        return ResponseEntity.ok(
                new GeneralResponse<>(new Result<>(20400, message, null, actionType), false)
        );
    }

    public static <T> ResponseEntity<GeneralResponse<T>> error(Integer code, String message) {
        return ResponseEntity.ok(
                new GeneralResponse<>(new Result<>(code, message, null, ActionTypeEnum.ERROR), false)
        );
    }

    public static <T> ResponseEntity<GeneralResponse<T>> error(Integer code, String message, ActionTypeEnum actionType) {
        return ResponseEntity.ok(
                new GeneralResponse<>(new Result<>(code, message, null, actionType), false)
        );
    }
}