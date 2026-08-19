package uz.tune.mentourBiz.rest.service.util.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.tune.mentourBiz.config.GlobalVar;
import uz.tune.mentourBiz.rest.domain.Message;
import uz.tune.mentourBiz.rest.enums.Lang;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.repository.MessageRepo;
import uz.tune.mentourBiz.rest.service.util.MessageSingleton;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MessageSingletonImpl implements MessageSingleton {

    private final MessageRepo messageRepo;

    @Override
    public String getMessage(String key, String defaultVal) {
        Lang currentLang = GlobalVar.getLang();

        // 1. Try requested language (e.g., TJK)
        Optional<Message> msg = messageRepo.findTopByKeyAndLang(key, currentLang);
        if (msg.isPresent()) return msg.get().getMessage();

        // 2. Fallback to English if the translation is missing
        if (currentLang != Lang.ENG) {
            msg = messageRepo.findTopByKeyAndLang(key, Lang.ENG);
            if (msg.isPresent()) return msg.get().getMessage();
        }

        // 3. If it's not even in the DB for English, use the default string from Java code
        // If defaultVal is null, return the key itself so you can see what is missing in the UI
        return (defaultVal != null) ? defaultVal : key;
    }

    @Override
    public String getMessage(MessageKey messageKey) {
        return getMessage(messageKey.getKey(), messageKey.getValue());
    }


    @Override
    public String getMessage(MessageKey key, Lang lang) {
        Optional<Message> messageOptional = messageRepo.findTopByKeyAndLang(key.getKey(), lang);
        if (messageOptional.isPresent()) {
            return messageOptional.get().getMessage();
        }
        return key.getValue();
    }
}
