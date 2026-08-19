package uz.tune.mentourBiz.rest.payload;

import lombok.Data;
import uz.tune.mentourBiz.rest.domain.SchoolDirector;

import java.util.UUID;

@Data
public class ResSchooLDirector {
    private String name;
    private String surName;
    private UUID uuid;

    public ResSchooLDirector(SchoolDirector schoolDirector) {
        this.name = schoolDirector.getUser().getFirstName();
        this.surName = schoolDirector.getUser().getLastName();
        this.uuid = schoolDirector.getUser().getUuid();
    }
}
