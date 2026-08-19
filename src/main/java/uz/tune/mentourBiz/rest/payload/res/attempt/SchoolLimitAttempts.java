package uz.tune.mentourBiz.rest.payload.res.attempt;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SchoolLimitAttempts {
    private boolean setAttempt;
    private int attempts;

    public SchoolLimitAttempts(boolean b, int schoolLimit) {
        this.setAttempt = b;
        this.attempts = schoolLimit;
    }
}
