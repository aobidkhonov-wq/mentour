package uz.tune.mentourBiz.rest.payload.res.lesson;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResStartMeeting {
    private UUID lessonId;
    private String lessonName;
    private String joinUrl;
    private String message;
}