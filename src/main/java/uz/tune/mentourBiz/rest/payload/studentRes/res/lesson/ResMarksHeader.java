package uz.tune.mentourBiz.rest.payload.studentRes.res.lesson;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class ResMarksHeader {
    private LocalDate date;
    private String unitTitle;
    private UUID unitUuid;
    // A lesson with no units still gets a column, so unitUuid alone cannot identify it.
    // Cells carry the same value in scheduleUUID, which is what the column matches on.
    private UUID lessonUuid;
}