package uz.tune.mentourBiz.rest.payload.res.school;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.Branch;

import java.util.UUID;

@Getter
@Setter
public class ResBranch {
    private UUID uuid;
    private String name;
    private String address;
    private String schoolName;

    public ResBranch(Branch branch) {
        this.uuid = branch.getUuid();
        this.name = branch.getName();
        this.address = branch.getAddress();
        if (branch.getSchool() != null) {
            this.schoolName = branch.getSchool().getName();
        }
    }
}