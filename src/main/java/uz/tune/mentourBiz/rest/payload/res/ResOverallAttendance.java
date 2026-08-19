package uz.tune.mentourBiz.rest.payload.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResOverallAttendance {
    private UUID studentUuid;
    private UUID groupUuid;
    private long totalLessons;
    private long attendedLessons;
    private double percentage;
}
