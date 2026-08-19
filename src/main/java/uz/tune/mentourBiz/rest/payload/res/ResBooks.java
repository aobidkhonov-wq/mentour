package uz.tune.mentourBiz.rest.payload.res;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.SchoolBook;
import uz.tune.mentourBiz.rest.payload.res.level.ResLevel;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ResBooks {
    private UUID bookUuid;
    private String bookName;
    private Boolean isGlobal;
    private ResLevel level;

    public ResBooks(SchoolBook schoolBook) {
        this.bookUuid = schoolBook.getUuid();
        this.bookName = schoolBook.getName();
        this.isGlobal = schoolBook.isGlobal();
        this.level = new ResLevel(schoolBook.getLevel());
    }
}
