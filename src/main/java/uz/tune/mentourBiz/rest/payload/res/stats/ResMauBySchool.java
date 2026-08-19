package uz.tune.mentourBiz.rest.payload.res.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResMauBySchool {

    private UUID schoolUuid;
    private String schoolName;
    private Long activeStudents;
    private Long activeTeachers;

}
