package uz.tune.mentourBiz.rest.service.subject;

import uz.tune.mentourBiz.rest.payload.req.subject.ReqSubject;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.subject.ResSubject;

import java.util.List;

public interface SubjectService {
    ResponseMessage createSubject(ReqSubject request);
    ResponseMessage updateSubject(Long subjectId, ReqSubject request);
    ResponseMessage deleteSubject(Long subjectId);
    ResSubject getSubject(Long subjectId);
    List<ResSubject> getSubjects();
}
