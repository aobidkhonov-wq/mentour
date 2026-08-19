package uz.tune.mentourBiz.rest.repository;

import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.TelegramBotUser;

import java.util.Optional;

public interface TelegramBotUserRepository extends BaseRepository<TelegramBotUser> {
    Optional<TelegramBotUser> findByPhoneNumberOrUsername(String phoneNumber, String username);
}