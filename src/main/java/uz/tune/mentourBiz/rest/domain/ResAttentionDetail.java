package uz.tune.mentourBiz.rest.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.service.ResGroupDetails;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResAttentionDetail {
    private UUID uuid;
    private String name;
    private ResGroupDetails group;

    public ResAttentionDetail(UUID uuid, String s) {
        this.uuid = uuid;
        this.name = s;
    }
}