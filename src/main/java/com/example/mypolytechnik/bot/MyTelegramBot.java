package com.example.mypolytechnik.bot;

import com.example.mypolytechnik.model.AdsRepository;
import com.example.mypolytechnik.model.Info_Change_Schedule;
import com.example.mypolytechnik.model.User;
import com.example.mypolytechnik.model.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;


@EnableScheduling
@Slf4j
@Component
public class MyTelegramBot extends TelegramLongPollingBot {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdsRepository adsRepository;
    private final String botUsername;
    private final String botToken;

    static final String HELP_TEXT = "Цей бот створений для студентів НУЛП (Дописати)";
    static final String ERROR_TEXT = "Error occurred: ";

    public MyTelegramBot(@Value("${bot.username}") String botUsername, @Value("${bot.token}") String botToken) {
        this.botUsername = botUsername;
        this.botToken = botToken;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("start", "Початок роботи в боті"));
        listOfCommands.add(new BotCommand("schedule", "Переглянути розклад"));
        listOfCommands.add(new BotCommand("task", "Переглянути завдань"));
        listOfCommands.add(new BotCommand("progress", "Переглянути успішність"));
        listOfCommands.add(new BotCommand("reminder", "Переглянути нагадування"));
        listOfCommands.add(new BotCommand("debt", "Переглянути заборгованість"));
        listOfCommands.add(new BotCommand("help", "Показати довідку"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Помилка встановлення списку команд бота: " + e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start":
                    registerUser(update.getMessage());
                    sendStartMessage(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case "/help":
                    sendTextMessage(chatId, HELP_TEXT);
                    break;
                default:
                    sendTextMessage(chatId, "Такої команди немає в списку :(");
            }
        }
    }

    private void registerUser(Message msg) {
        if (userRepository.findById(msg.getChatId()).isEmpty()) {
            var chatId = msg.getChatId();
            var chat = msg.getChat();

            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegistredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("Користувач збережений: " + user);
        }
    }

    private void sendStartMessage(long chatId, String firstName) {
        String messageText = "Привіт, " + firstName + ", раді бачити тебе!";
        log.info("Відповідь користувачу " + firstName);
        sendTextMessage(chatId, messageText);
    }

    private void sendTextMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Розклад");
        row.add("Завдання");
        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add("Успішність");
        row.add("Нагадування");
        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add("Заборгованість");
        row.add("Про чат-бот");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);

        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Виникла помилка: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    private void executeMessage(SendMessage message){
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void prepareAndSendMessage(long chatId, String textToSend){
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }

    @Scheduled(cron = "${cron.scheduler}")
    private void sendAds(){
        var ads=adsRepository.findAll();
        var users=userRepository.findAll();

        for(Info_Change_Schedule ad:ads){
            for(User user:users){
                prepareAndSendMessage(user.getChatId(),ad.getAd());
            }
        }

    }
}
