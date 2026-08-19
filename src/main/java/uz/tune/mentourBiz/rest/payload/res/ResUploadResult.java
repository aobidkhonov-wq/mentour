package uz.tune.mentourBiz.rest.payload.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uz.tune.mentourBiz.rest.model.FailedStudentModel;

import java.util.List;

@Getter
@AllArgsConstructor
public class ResUploadResult {
    private List<FailedStudentModel> failedStudents;
}