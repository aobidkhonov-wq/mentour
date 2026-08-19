package uz.tune.mentourBiz.rest.payload.req.parent;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqChangeParentPassword {
    @NotBlank
    private String currentPassword;

    @NotBlank
    private String newPassword;
}
