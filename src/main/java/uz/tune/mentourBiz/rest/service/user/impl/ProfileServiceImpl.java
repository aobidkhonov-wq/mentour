package uz.tune.mentourBiz.rest.service.user.impl;


import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.Attachment;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.payload.req.profile.ReqProfileChangePassword;
import uz.tune.mentourBiz.rest.payload.req.profile.ReqProfileUpdateInfo;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.profile.ResProfileInfo;
import uz.tune.mentourBiz.rest.payload.res.profile.ResStudentProfileInfo;
import uz.tune.mentourBiz.rest.repository.AttachmentRepo;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.user.UserRepo;
import uz.tune.mentourBiz.rest.service.user.ProfileService;
import uz.tune.mentourBiz.rest.service.user.UserService;
import uz.tune.mentourBiz.rest.service.util.MessageSingleton;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserService userService;
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final MessageSingleton messageSingleton;
    private final StudentRepo studentRepo;
    private final AttachmentRepo attachmentRepo;

    @Override
    public ResProfileInfo getProfileInfo() {
        User currentUser = userService.getCurrentUser();
        if (currentUser.getRole().equals(UserRole.STUDENT)) {
            Student student = studentRepo.findByUser(currentUser).orElse(null);
            return new ResProfileInfo(currentUser, student);
        }
        return new ResProfileInfo(currentUser);
    }

    @Override
    public ResStudentProfileInfo getStudentProfileInfo() {
        User currentUser = userService.getCurrentUser();
        Student student = studentRepo.findByUserUuid(currentUser.getUuid())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        School school = student.getSchool();
        return new ResStudentProfileInfo(currentUser, school);
    }

    @Override
    public ResponseMessage updateProfileInfo(ReqProfileUpdateInfo request) {
        User currentUser = userService.getCurrentUser();
        currentUser.setFirstName(request.getFirstName());
        currentUser.setLastName(request.getLastName());

        if (request.getImageId() != null) {
            Attachment photo = attachmentRepo.findByUuid(request.getImageId())
                    .orElseThrow(() -> new EntityNotFoundException("Image not found"));
            currentUser.setAttachment(photo);
        }

        userRepo.save(currentUser);
        return new ResponseMessage("Profile updated successfully");
    }

    @Override
    public ResponseMessage changePassword(ReqProfileChangePassword request) {
        User currentUser = userService.getCurrentUser();

        if (!passwordEncoder.matches(request.getOldPassword(), currentUser.getPassword())) {
            throw new ValidationException(MessageKey.OLD_PASSWORD_IS_INCORRECT.getKey());
        }

        if (!Objects.equals(request.getNewPassword(), request.getConfirmPassword())) {
            throw new ValidationException(MessageKey.NEW_PASSWORDS_DO_NOT_MATCH.getKey());
        }

        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepo.save(currentUser);
        return new ResponseMessage("Password changed successfully");
    }


}