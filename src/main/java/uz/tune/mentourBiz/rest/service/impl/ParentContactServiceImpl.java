package uz.tune.mentourBiz.rest.service.impl;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.StudentParentContact;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.payload.req.ReqAddParents;
import uz.tune.mentourBiz.rest.payload.req.ReqUpdateParent;
import uz.tune.mentourBiz.rest.payload.res.ResParentDetail;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.repository.StudentParentContactRepository;
import uz.tune.mentourBiz.rest.repository.TelegramBotUserRepository;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.user.UserRepo;
import uz.tune.mentourBiz.rest.service.ParentContractService;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParentContactServiceImpl implements ParentContractService  {

    private final StudentParentContactRepository parentRepo;
    private final StudentRepo studentRepo;
    private final AuthToViewEntity authToViewEntity;
    private final TelegramBotUserRepository telegramBotUserRepository;
    private final UserRepo userRepo;

    @Transactional
    @Override
    public ResponseMessage addParentsBatch(ReqAddParents req) {
        Student student = studentRepo.findByUuid(req.getStudentUuid())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        authToViewEntity.authorizeActionUponStudent(student);

        if (req.getParents() == null || req.getParents().isEmpty()) {
            throw new ValidationException(MessageKey.LIST_EMPTY.getKey());
        }

        List<StudentParentContact> toSave = new ArrayList<>();
        for (ReqAddParents.ParentDetail p : req.getParents()) {
            validateParentInput(p.getName(), p.getTelegramNickname(), p.getPhoneNumber());

            StudentParentContact contact = new StudentParentContact();
            contact.setStudent(student);
            contact.setLanguage(p.getLanguage()); //
            contact.setParentName(p.getName());
            contact.setTelegramNickname(p.getTelegramNickname());
            contact.setTelegramChatId(null);
            contact.setPhoneNumber(p.getPhoneNumber());
            contact.setUseTelegram(p.isUseTelegram());
            contact.setUseWhatsapp(p.isUseWhatsapp());

            telegramBotUserRepository.findByPhoneNumberOrUsername(p.getPhoneNumber(), p.getTelegramNickname()).ifPresent(botUser -> {
                contact.setTelegramChatId(botUser.getChatId());
            });

            toSave.add(contact);
        }
        parentRepo.saveAll(toSave);
        return new ResponseMessage("Successfully added " + toSave.size() + " parent(s).");
    }

    @Override
    public List<ResParentDetail> getParentsByStudent(UUID studentUuid) {
        Student student = studentRepo.findByUuid(studentUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));


        authToViewEntity.authorizeActionUponStudent(student);

        return parentRepo.findAllByStudent(student).stream()
                .map(ResParentDetail::new)
                .toList();
    }

    @Transactional
    @Override
    public ResParentDetail updateParent(UUID contactUuid, ReqUpdateParent req) {
        StudentParentContact contact = parentRepo.findByUuid(contactUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.PARENT_CONTACT_REQUIRED.getKey()));

        authToViewEntity.authorizeActionUponStudent(contact.getStudent());

        if (req.getName() != null) contact.setParentName(req.getName());
        if (req.getLanguage() != null) contact.setLanguage(req.getLanguage());
        if (req.getIsActive() != null) contact.setActive(req.getIsActive());

        String nickname = req.getTelegramNickname() != null ? req.getTelegramNickname() : contact.getTelegramNickname();
        String phone = req.getPhoneNumber() != null ? req.getPhoneNumber() : contact.getPhoneNumber();
        String cleanPhone = (phone != null) ? phone.replace("+", "").trim() : null;

        validateParentInput(contact.getParentName(), nickname, phone);

        contact.setTelegramNickname(nickname);
        contact.setPhoneNumber(phone);

        telegramBotUserRepository.findByPhoneNumberOrUsername(cleanPhone, nickname)
                .ifPresent(botUser -> {
                    contact.setTelegramChatId(botUser.getChatId());
                    if (botUser.getLanguage() != null) {
                        contact.setLanguage(botUser.getLanguage());
                    }
                });

        return new ResParentDetail(parentRepo.save(contact));
    }

    @Transactional
    @Override
    public ResponseMessage removeParentLink(UUID contactUuid) {
        StudentParentContact contact = parentRepo.findByUuid(contactUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.PARENT_CONTACT_REQUIRED.getKey()));

        authToViewEntity.authorizeActionUponStudent(contact.getStudent());
        parentRepo.delete(contact);
        return new ResponseMessage("Parent contact removed.");
    }

    private void validateParentInput(String name, String nickname, String phone) {
        if (CoreUtils.isEmpty(name)) {
            throw new ValidationException(MessageKey.PARENT_NAME_REQUIRED.getKey());
        }
        if (CoreUtils.isEmpty(nickname) && CoreUtils.isEmpty(phone)) {
            throw new ValidationException(MessageKey.PARENT_CONTACT_REQUIRED.getKey());
        }
    }
}