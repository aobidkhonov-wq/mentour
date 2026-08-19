package uz.tune.mentourBiz.rest.payload.res.student;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResBulkStudentResult {
    private int created;
    private List<Failed> failed;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Failed {
        private String username;
        private String reason;
    }
}
