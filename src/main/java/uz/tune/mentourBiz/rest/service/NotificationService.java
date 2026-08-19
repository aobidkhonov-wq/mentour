package uz.tune.mentourBiz.rest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.config.logger.Logger;
import uz.tune.mentourBiz.rest.domain.Notification;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.NotificationTargetType;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.repository.*;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.user.UserRepo;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;
import uz.tune.mentourBiz.utils.FcmPushService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserRepo userRepo;
    private final FcmTokenRepo fcmTokenRepo;
    private final FcmPushService fcmPushService;
    private final NotificationRepo notificationRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final TelegramBotService telegramBotService;
    private final StudentParentContactRepository studentParentContactRepository;
    private final UserScopeService userScopeService;
    private final UserService userService;
    private final SchoolDirectorRepo schoolDirectorRepo;
    private final AttachmentRepo attachmentRepo;

    @Transactional
    public void sendGlobalNotification(String title, String content, NotificationTargetType type, UUID targetUuid, UUID attachmentUuid) {
        User currentUser = userService.getCurrentUser();
        Notification n = new Notification();
        n.setTitle(title);
        n.setContent(content);
        n.setTargetType(type);
        n.setTargetUuid(targetUuid);
        n.setCreatedBy(currentUser != null ? currentUser.getFirstName() + " " + currentUser.getLastName() : "SYSTEM");

        if (attachmentUuid != null) {
            attachmentRepo.findByUuid(attachmentUuid).ifPresent(n::setAttachment);
        }

        notificationRepo.save(n);

        // 1. Calculate the target logic outside the lambda
        UUID tempUuid = targetUuid;
        if (currentUser.getRole().equals(UserRole.SCHOOL_DIRECTOR) && targetUuid == null) {
            // Keep null for Org-wide logic
        } else if (!currentUser.getRole().equals(UserRole.SYS_ADMIN) && targetUuid == null) {
            tempUuid = userScopeService.getCurrentUserSchoolUuid();
        }

        // 2. Create an effectively final variable for the lambda
        final UUID asyncTargetUuid = tempUuid;

        CompletableFuture.runAsync(() -> {
            try {
                List<String> fcmTokens = new ArrayList<>();
                List<String> telegramIds = new ArrayList<>();

                switch (type) {
                    case BROADCAST_ALL -> {
                        fcmTokens = fcmTokenRepo.findAllActiveTokens();
                        telegramIds = userRepo.findAllActiveTelegramChatIds();
                    }
                    case ORGANIZATION_WIDE -> {
                        UUID orgUuid = (currentUser.getRole() == UserRole.SCHOOL_DIRECTOR) ?
                                schoolDirectorRepo.findByUser(currentUser).get().getOrganization().getUuid() : asyncTargetUuid;

                        fcmTokens = fcmTokenRepo.findTokensByOrganization(orgUuid);
                        telegramIds = userRepo.findChatIdsByOrganization(orgUuid);
                        telegramIds.addAll(studentParentContactRepository.findParentChatIdsByOrganization(orgUuid));
                    }
                    case SCHOOL_WIDE -> {
                        UUID resolvedTarget = userScopeService.resolveSchoolUuid(asyncTargetUuid);
                        if (resolvedTarget == null) return;

                        fcmTokens = fcmTokenRepo.findTokensBySchool(resolvedTarget);
                        telegramIds = userRepo.findChatIdsBySchool(resolvedTarget);
                        telegramIds.addAll(studentParentContactRepository.findParentChatIdsBySchool(resolvedTarget));
                    }
                    case GROUP_WIDE -> {
                        fcmTokens = fcmTokenRepo.findTokensByGroup(asyncTargetUuid);
                        telegramIds = userRepo.findChatIdsByGroup(asyncTargetUuid);
                    }
                    case INDIVIDUAL_USER -> {
                        fcmTokens = fcmTokenRepo.findTokensByUserUuids(Collections.singletonList(asyncTargetUuid));
                        telegramIds = userRepo.findChatIdByUserUuid(asyncTargetUuid);
                    }
                }

                if (!fcmTokens.isEmpty()) fcmPushService.sendPush(fcmTokens, title, content);

                String tgMsg = "<b>" + title + "</b>\n\n" + content;
                for (String chatId : telegramIds) {
                    if (chatId != null) telegramBotService.sendMsg(chatId, tgMsg);
                }

            } catch (Exception e) {
                Logger.exception("Notification Delivery Failed", e);
            }
        });
    }
}