package uz.tune.mentourBiz.rest.service;

public interface TelegramService {
    void sendMsg(String chatId, String text);
}
