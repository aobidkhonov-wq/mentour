package uz.tune.mentourBiz.rest.service.subject;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Subject;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.payload.req.subject.ReqSubject;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.subject.ResSubject;
import uz.tune.mentourBiz.rest.repository.group.LevelRepository;
import uz.tune.mentourBiz.rest.repository.group.SubjectRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final LevelRepository levelRepository;

    @Override
    @Transactional
    public ResponseMessage createSubject(ReqSubject request) {
        if (subjectRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ValidationException(MessageKey.SUBJECT_NAME_TAKEN.getKey());
        }

        Subject subject = new Subject();
        subject.setName(request.getName());
        subject.setIsGlobal(request.getIsGlobal() != null && request.getIsGlobal());
        subjectRepository.save(subject);

        return new ResponseMessage("SUCCESS");
    }

    @Override
    @Transactional
    public ResponseMessage updateSubject(Long subjectId, ReqSubject request) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SUBJECT_NOT_FOUND.getKey()));

        if (request.getName() != null) {
            if (subjectRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), subjectId)) {
                throw new ValidationException(MessageKey.SUBJECT_NAME_TAKEN.getKey());
            }
            subject.setName(request.getName());
        }
        if (request.getIsGlobal() != null) {
            subject.setIsGlobal(request.getIsGlobal());
        }
        subjectRepository.save(subject);

        return new ResponseMessage("SUCCESS");
    }

    @Override
    @Transactional
    public ResponseMessage deleteSubject(Long subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SUBJECT_NOT_FOUND.getKey()));

        if (levelRepository.existsBySubject_Id(subjectId)) {
            throw new ValidationException(MessageKey.SUBJECT_IN_USE.getKey());
        }

        subjectRepository.delete(subject);
        return new ResponseMessage("SUCCESS");
    }

    @Override
    public ResSubject getSubject(Long subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SUBJECT_NOT_FOUND.getKey()));
        return new ResSubject(subject);
    }

    @Override
    public List<ResSubject> getSubjects() {
        return subjectRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream().map(ResSubject::new).toList();
    }
}
