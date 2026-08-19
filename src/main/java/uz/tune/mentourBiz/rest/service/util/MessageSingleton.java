package uz.tune.mentourBiz.rest.service.util;

import uz.tune.mentourBiz.rest.enums.Lang;
import uz.tune.mentourBiz.rest.enums.MessageKey;

public interface MessageSingleton {

    String getMessage(String key, String title);

    String getMessage(MessageKey messageKey);

    String getMessage(MessageKey messageKey, Lang lang);

}
