package uz.tune.mentourBiz.rest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import uz.tune.mentourBiz.config.logger.Logger;
import uz.tune.mentourBiz.rest.domain.StudentParentContact;
import uz.tune.mentourBiz.rest.domain.TelegramBotUser;
import uz.tune.mentourBiz.rest.enums.Lang;
import uz.tune.mentourBiz.rest.repository.StudentParentContactRepository;
import uz.tune.mentourBiz.rest.repository.TelegramBotUserRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TelegramBotService extends TelegramLongPollingBot implements TelegramService {

    @Value("${app.telegram.bot-token}")
    private String botToken;
    @Value("${app.telegram.bot-username}")
    private String botUsername;

    private final TelegramBotUserRepository botUserRepo;
    private final StudentParentContactRepository parentRepo;

    @Override public String getBotToken() { return botToken; }
    @Override public String getBotUsername() { return botUsername; }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            String chatId = update.getMessage().getChatId().toString();
            String text = update.getMessage().getText();
            String userName = update.getMessage().getFrom().getUserName();

            if (update.getMessage().hasContact()) {
                handleContact(chatId, update.getMessage().getContact(), userName);
            }
            else if (text != null && (text.startsWith("/start") || text.startsWith("/lang"))) {
                sendLanguageSelector(chatId);
            }
        }
        else if (update.hasCallbackQuery()) {
            handleLanguageCallback(update);
        }
    }

    private void sendLanguageSelector(String chatId) {
        SendMessage sm = new SendMessage();
        sm.setChatId(chatId);
        sm.setText("🌐 Please select your language / Tilni tanlang / Выберите язык / Забонро интихоб кунед / Тилди тандаңыз:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createLangButton("🇺🇿 O'zbekcha", "setlang_UZB"));
        row1.add(createLangButton("🇷🇺 Русский", "setlang_RUS"));
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createLangButton("🇹🇯 Тоҷикӣ", "setlang_TJK"));
        row2.add(createLangButton("🇰🇬 Кыргызcha", "setlang_KRG"));
        rows.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createLangButton("🇬🇧 English", "setlang_ENG"));
        rows.add(row3);

        markup.setKeyboard(rows);
        sm.setReplyMarkup(markup);
        try { execute(sm); } catch (Exception e) { e.printStackTrace(); }
    }

    private InlineKeyboardButton createLangButton(String text, String data) {
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText(text);
        btn.setCallbackData(data);
        return btn;
    }

    private void handleLanguageCallback(Update update) {
        String data = update.getCallbackQuery().getData();
        String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
        String userName = update.getCallbackQuery().getFrom().getUserName();

        Logger.logInfo(data);
        Logger.logInfo(chatId);
        Logger.logInfo(userName);
        if (data.startsWith("setlang_")) {
            Lang selectedLang = Lang.valueOf(data.split("_")[1]);
            Logger.logInfo(selectedLang.toString());

            // f
            TelegramBotUser botUser = botUserRepo.findByPhoneNumberOrUsername("NONE", userName)
                    .orElse(new TelegramBotUser());
            Logger.logInfo(botUser.toString());
            botUser.setChatId(chatId);
            botUser.setUsername(userName);
            botUser.setLanguage(selectedLang);
            botUserRepo.save(botUser);

            List<StudentParentContact> linkedContacts = parentRepo.findAllByPhoneNumberOrTelegramNickname("NONE", userName);
            for (StudentParentContact c : linkedContacts) {
                Logger.logInfo("FOUND" + linkedContacts.toString());
                Logger.logInfo(c.toString());
                c.setLanguage(selectedLang);
                parentRepo.save(c);
            }

            String confirmation = switch (selectedLang) {
                case RUS -> "Язык изменен на Русский 🇷🇺";
                case TJK -> "Забон ба Тоҷикӣ иваз карда шуд 🇹🇯";
                case KRG -> "Тил Кыргызчага өзгөртүлдү 🇰🇬";
                case ENG -> "Language changed to English 🇬🇧";
                default -> "Til O'zbekchaga o'zgartirildi 🇺🇿";
            };

            sendSimpleMsg(chatId, "✅ " + confirmation);

            if (botUser.getPhoneNumber() == null || botUser.getPhoneNumber().equals("NONE")) {
                sendRegisterButton(chatId, selectedLang);
            }
        }
    }

    private void handleContact(String chatId, Contact contact, String userName) {
        String phone = contact.getPhoneNumber().replace("+", "");

        TelegramBotUser botUser = botUserRepo.findByPhoneNumberOrUsername(phone, userName)
                .orElse(new TelegramBotUser());
        botUser.setChatId(chatId);
        botUser.setPhoneNumber(phone);
        botUser.setUsername(userName);
        botUserRepo.save(botUser);

        List<StudentParentContact> contacts = parentRepo.findAllByPhoneNumberOrTelegramNickname(phone, userName);
        for (StudentParentContact c : contacts) {
            c.setTelegramChatId(chatId);
            if (botUser.getLanguage() != null) {
                c.setLanguage(botUser.getLanguage());
            }
            parentRepo.save(c);
        }

        String successMsg = switch (botUser.getLanguage() != null ? botUser.getLanguage() : Lang.UZB) {
            case RUS -> "<b>Успешно!</b> Вы будете получать уведомления здесь.";
            case TJK -> "<b>Муваффақият!</b> Шумо огоҳиномаҳоро дар ин ҷо мегиред.";
            case KRG -> "<b>Ийгиликтүү!</b> Сиз бул жерден эскертмелерди аласыз.";
            case ENG -> "<b>Success!</b> You will receive notifications here.";
            default -> "<b>Muvaffaqiyatli!</b> Bildirishnomalarni shu yerda olasiz.";
        };

        sendSimpleMsg(chatId, "✅ " + successMsg);
    }

    private void sendRegisterButton(String chatId, Lang lang) {
        String promptText = switch (lang) {
            case RUS -> "Для завершения нажмите кнопку ниже:";
            case TJK -> "Барои анҷом додан тугмаи зерро пахш кунед:";
            case KRG -> "Аяктоо үчүн төмөнкү баскычты басыңыз:";
            case ENG -> "To complete, please click the button below:";
            default -> "Tugallash uchun pastdagi tugmani bosing:";
        };

        String btnText = switch (lang) {
            case RUS -> "📱 Поделиться контактом";
            case TJK -> "📱 Рақами телефон";
            case KRG -> "📱 Телефон номерин жөнөтүү";
            case ENG -> "📱 Share Contact";
            default -> "📱 Kontaktingizni yuboring";
        };

        SendMessage sm = new SendMessage();
        sm.setChatId(chatId);
        sm.setText(promptText);

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        KeyboardButton button = new KeyboardButton(btnText);
        button.setRequestContact(true);
        row.add(button);
        rows.add(row);
        markup.setKeyboard(rows);
        sm.setReplyMarkup(markup);

        try { execute(sm); } catch (Exception e) { e.printStackTrace(); }
    }

    private void sendSimpleMsg(String chatId, String text) {
        SendMessage sm = new SendMessage(chatId, text);
        sm.setParseMode(ParseMode.HTML);
        try { execute(sm); } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void sendMsg(String chatId, String text) {
        sendSimpleMsg(chatId, text);
    }
}