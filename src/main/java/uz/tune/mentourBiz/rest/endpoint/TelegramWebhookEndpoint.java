package uz.tune.mentourBiz.rest.endpoint;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.config.logger.Logger;
import uz.tune.mentourBiz.rest.service.TelegramBotService;

@RestController
@RequestMapping(BaseURI.API1 + "/public/telegram-webhook")
public class TelegramWebhookEndpoint {
    private final TelegramBotService telegramBotService;

    public TelegramWebhookEndpoint(TelegramBotService parentBotService) {
        this.telegramBotService = parentBotService;
    }

    @PostMapping
    public void receiveUpdate(@RequestBody Update update) {
        try {
            telegramBotService.onUpdateReceived(update);
        } catch (Exception e) {
            Logger.logWarn("Error handling Telegram webhook update");
        }
    }
}