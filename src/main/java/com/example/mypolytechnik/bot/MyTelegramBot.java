package com.example.mypolytechnik.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class MyTelegramBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final String botToken;

    static final String HELP_TEXT="Цей бот створений для студентів НУЛП (Дописати)";
    public MyTelegramBot(@Value("${bot.username}") String botUsername, @Value("${bot.token}") String botToken) {
        this.botUsername = botUsername;
        this.botToken = botToken;
        List<BotCommand> listofCommands = new ArrayList<>();
        listofCommands.add(new BotCommand("/start", "get a welcome message"));
        listofCommands.add(new BotCommand("/mydata", "get your data stored"));
        listofCommands.add(new BotCommand("/deletedata", "delete my data"));
        listofCommands.add(new BotCommand("/help", "show help"));
        listofCommands.add(new BotCommand("/settings", "show your preferences"));
        try {
            this.execute(new SetMyCommands(listofCommands, new BotCommandScopeDefault(),null));
        }catch(TelegramApiException e){
            log.error("Error setting bot's command list: "+e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start":
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

    private void sendStartMessage(long chatId, String firstName) {
        String messageText = "Привіт, " + firstName + ", раді бачити тебе!";
        log.info("Replied to user "+firstName);
        sendTextMessage(chatId, messageText);
    }

    private void sendTextMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred"+e.getMessage());
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
}
