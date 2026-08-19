package uz.tune.mentourBiz.rest.payload.studentRes.res.lesson;


import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
@Getter
@Setter
public class ResMarksDashboard {
    private List<ResMarksHeader> headers;
    private List<ResStudentMarksRow> studentRows;
    private LocalDate courseStartDate;
    private LocalDate courseEndDate;
}