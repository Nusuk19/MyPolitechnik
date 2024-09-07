package com.example.mypolytechnik.bot;

import com.example.mypolytechnik.bot.task.Task;
import com.example.mypolytechnik.bot.task.TaskRepository;
import com.example.mypolytechnik.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


@EnableScheduling
@Slf4j
@Component
public class MyTelegramBot extends TelegramLongPollingBot {
    @Autowired
    private UserRepository userRepository;

    private Map<Long, Long> pendingFileSubjects = new HashMap<>();

    @Autowired
    private UserFileRepository userFileRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private AdsRepository adsRepository;
    private final String botUsername;
    private final String botToken;
    private ScheduleManager scheduleManager;

    static final String HELP_TEXT = "Цей бот створений для студентів Національного університету \"Львівська політехніка\". Він допомагає студентам в управлінні їхнім навчальним процесом, надаючи можливість переглядати розклад занять, додавати та переглядати завдання, додавати файли для збереження (наприклад лабораторні роботи), а також отримувати різноманітні нагадування, відстежувати свою успішність та заборгованість, переглядати пріоритетність завдань";
    static final String ERROR_TEXT = "Error occurred: ";

    public MyTelegramBot(@Value("${bot.username}") String botUsername, @Value("${bot.token}") String botToken) {
        this.botUsername = botUsername;
        this.botToken = botToken;
        this.scheduleManager = new ScheduleManager(this);
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("start", "Початок роботи в боті"));
        listOfCommands.add(new BotCommand("schedule", "Переглянути розклад"));
        listOfCommands.add(new BotCommand("task", "Переглянути завдань"));
        listOfCommands.add(new BotCommand("myfiles", "Перегляд завантажених файлів"));
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
            Message message = update.getMessage();

            if (pendingFileSubjects.containsKey(chatId)) {
                long fileId = pendingFileSubjects.remove(chatId);
                Optional<UserFile> optionalUserFile = userFileRepository.findById(fileId);
                if (optionalUserFile.isPresent()) {
                    UserFile userFile = optionalUserFile.get();
                    userFile.setSubject(messageText);
                    userFileRepository.save(userFile);
                    sendTextMessage(chatId, "Предмет збережено: " + messageText);
                } else {
                    sendTextMessage(chatId, "Помилка збереження предмету.");
                }
            }else {

                switch (messageText) {
                    case "/start":
                        registerUser(update.getMessage());
                        sendStartMessage(chatId, update.getMessage().getChat().getFirstName());
                        sendWelcomeMessage(chatId);
                        break;
                    case "/schedule":
                        scheduleManager.handleScheduleCommand(chatId);
                        break;
                    case "/task":
                        listUserTasks(chatId);
                        break;
                    case "Додати завдання":
                        sendTextMessage(chatId, "Для створення завдання використовуйте такий ввід даних: /addtask <тема> | <опис> | <дедлайн> , зазначаємо, що дедлайн у такому вигляді 2024-05-23 14:30 ");
                        break;
                    case "Мої завдання":
                        listUserTasks(chatId);
                        break;
                    case "Завершити завдання":
                        sendTextMessage(chatId, "Введіть номер завдання, яке ви хочете видалити, як команду (/del <ID завдання>:) ");
                        break;
                    case "Змінити завдання":
                        sendTextMessage(chatId, "Будь ласка, введіть ID завдання, яке ви хочете позначити як виконане, як команду (/com <ID завдання>):");
                        break;
                    case "Протерміновані завдання":
                        listExpiredTasks(chatId);
                        break;
                    case "Пріоритетність":
                        listTaskPriority(chatId);
                        break;
                    case "Терміни здачі":
                        listTaskTopicsAndDeadlines(chatId);
                        break;
                    case "Видалити файл":
                        sendTextMessage(chatId, "Будь ласка, введіть ID файла, який ви хочете видалити, як команду (/delfile <ID завдання>):");
                        break;
                    case "/myfiles":
                        listUserFiles(chatId);
                        break;
                    case "/progress":
                        break;
                    case "/reminder":
                        break;
                    case "/debt":
                        listExpiredTasks(chatId);
                        break;
                    case "/help":
                        sendTextMessage(chatId, HELP_TEXT);
                        break;
                    case "Розклад":
                        List<String> scheduleButtons = Arrays.asList("Переглянути розклад", "Встановити розклад", "Назад");
                        sendCustomKeyboard(chatId, scheduleButtons);
                        break;
                    case "Переглянути розклад":
                        scheduleManager.handleScheduleCommand(chatId);
                        break;
                    case "Заборгованість":
                        List<String> scheduleButtons1 = Arrays.asList("Протерміновані завдання", "Пріоритетність", "Назад");
                        sendCustomKeyboard(chatId, scheduleButtons1);
                        break;
                    case "Успішність":
                        List<String> scheduleButtons2 = Arrays.asList("Статистика", "Мої оцінки", "Назад");
                        sendCustomKeyboard(chatId, scheduleButtons2);
                        break;
                    case "Нагадування":
                        List<String> scheduleButtons3 = Arrays.asList("Налаштування сповіщень", "Терміни здачі", "Назад");
                        sendCustomKeyboard(chatId, scheduleButtons3);
                        break;
                    case "Завдання":
                        List<String> scheduleButtons4 = Arrays.asList("Мої завдання", "Додати завдання", "Завершити завдання", "Змінити завдання","Видалити файл", "Назад");
                        sendCustomKeyboard(chatId, scheduleButtons4);
                        break;
                    case "Про чат-бот":
                        sendTextMessage(chatId, HELP_TEXT);
                        break;
                    case "Назад":
                        sendTextMessage(chatId, "Виберіть розділ: ");
                        break;
                }
            }
            // Якщо користувач надіслав повідомлення після команди "Додати завдання", це може бути введенням для завдання
            if (update.getMessage().hasText() && messageText.contains("|")) {
                addTaskFromInput(chatId, messageText);
            }
            if (messageText.startsWith("/delfile")) {
                long fileId = Long.parseLong(messageText.substring("/delfile ".length()).trim());
                deleteFile(chatId, fileId);
            }
            if (messageText.startsWith("/del")) {
                long taskId = Long.parseLong(messageText.substring("/del ".length()).trim());
                deleteTask(chatId, taskId);
            }
            // Перевіряємо, чи вхідне повідомлення містить команду для позначення завдання як виконане
            else if (messageText.startsWith("/com")) {
                long taskId = Long.parseLong(messageText.substring("/com ".length()).trim());
                markTaskAsCompleted(chatId, taskId);
            }


        }
        if (update.hasMessage() && update.getMessage().hasDocument()) {
            handleDocumentUpload(update.getMessage());
        }

    }

    private void sendWelcomeMessage(long chatId) {
        String instructions = "Вітаємо у нашому боті! Ось як ви можете використовувати наш бот:\n\n" +
                "1. Щоб завантажити файл, просто надішліть його боту.\n" +
                "   - Відкрийте діалог з ботом.\n" +
                "   - Натисніть на значок скріпки (📎).\n" +
                "   - Виберіть файл, який бажаєте завантажити.\n" +
                "   - Надішліть файл боту.\n" +
                "2. Після завантаження файлу бот запитає вас про назву предмету. Введіть назву предмету у відповідь.\n" +
                "3. Щоб переглянути усі завантажені вами файли, використовуйте команду /myfiles.\n" +
                "4. Ви також можете переглянути свої завдання та інші функції за допомогою відповідних команд та кнопок.";

        sendTextMessage(chatId, instructions);
    }

    private void deleteFile(long chatId, Long fileId) {
        Optional<UserFile> optionalUserFile = userFileRepository.findById(fileId);
        if (optionalUserFile.isPresent()) {
            userFileRepository.deleteById(fileId);
            sendTextMessage(chatId, "Файл успішно видалено.");
        } else {
            sendTextMessage(chatId, "Файл з вказаним номером не знайдено.");
        }
    }

    private void handleDocumentUpload(Message message) {
        long chatId = message.getChatId();
        Document document = message.getDocument();
        String fileId = document.getFileId();
        String fileName = document.getFileName();

        // Збереження документа у базі даних
        UserFile userFile = new UserFile();
        userFile.setChatId(chatId);
        userFile.setFileId(fileId);
        userFile.setFileName(fileName);
        userFile.setUploadedAt(new Timestamp(System.currentTimeMillis()));

        UserFile savedFile = userFileRepository.save(userFile);

        // Зберігаємо ID файлу для подальшого оновлення предмету
        pendingFileSubjects.put(chatId, savedFile.getId());

        sendTextMessage(chatId, "Файл отримано. Введіть назву предмету:");
    }

    private void listUserFiles(long chatId) {
        List<UserFile> userFiles = userFileRepository.findByChatId(chatId);
        if (userFiles.isEmpty()) {
            sendTextMessage(chatId, "У вас немає завантажених файлів.");
        } else {
            for (UserFile userFile : userFiles) {
                SendDocument sendDocument = new SendDocument();
                sendDocument.setChatId(String.valueOf(chatId));
                sendDocument.setDocument(new InputFile(userFile.getFileId()));
                sendDocument.setCaption("Предмет: " + userFile.getSubject() + "\nФайл: " + userFile.getFileName()+"\nID: " + userFile.getId());

                try {
                    execute(sendDocument);
                } catch (TelegramApiException e) {
                    log.error("Помилка відправки файлу: " + e.getMessage());
                }
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


    private void addTaskFromInput(long chatId, String messageText) {
        String[] parts = messageText.substring(9).split("\\|");
        if (parts.length == 3) {
            addTask(chatId, parts[0].trim(), parts[1].trim(), parts[2].trim());
        } else {
            sendTextMessage(chatId, "Невірний формат. Використовуйте: /addtask <тема> | <опис> | <дедлайн> , зазначаємо, що дедлайн у такому вигляді 2024-05-23 14:30 ");
        }
    }

    private void listUserTasks(long chatId) {
        List<Task> tasks = taskRepository.findByChatId(chatId);
        StringBuilder response = new StringBuilder("Ваші завдання:\n");
        Integer i = 0;
        for (Task task : tasks) {
            i++;
            String status = task.isCompleted() ? "виконано" : "не виконано";
            response.append(i).append(") ID Завдання: ").append(task.getId()).append("\n")
                    .append("Тема: ").append(task.getTitle()).append("\n")
                    .append("Опис: ").append(task.getDescription()).append("\n")
                    .append("Дедлайн: ").append(task.getDueDate()).append("\n")
                    .append("Стан: ").append(status).append("\n\n");
        }
        sendTextMessage(chatId, response.toString());
    }

    private void addTask(long chatId, String title, String description, String dueDateStr) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Timestamp dueDate;
        try {
            dueDate = new Timestamp(dateFormat.parse(dueDateStr).getTime());
        } catch (ParseException e) {
            sendTextMessage(chatId, "Невірний формат дати. Використовуйте: yyyy-MM-dd HH:mm");
            return;
        }

        Task task = new Task();
        task.setChatId(chatId);
        task.setTitle(title);
        task.setDescription(description);
        task.setDueDate(dueDate);
        task.setCompleted(false);
        taskRepository.save(task);
        sendTextMessage(chatId, "Завдання додано: " + title);
    }

    private void deleteTask(long chatId, Long taskId) {
        Optional<Task> optionalTask = taskRepository.findById(taskId);
        if (optionalTask.isPresent()) {
            taskRepository.deleteById(taskId);
            sendTextMessage(chatId, "Завдання успішно видалено.");
        } else {
            sendTextMessage(chatId, "Завдання з вказаним номером не знайдено.");
        }
    }


    private void markTaskAsCompleted(long chatId, Long taskId) {
        // Тепер ми можемо позначити завдання як виконане
        Optional<Task> taskOptional = taskRepository.findById(taskId);
        if (taskOptional.isPresent()) {
            Task task = taskOptional.get();
            if (!task.isCompleted()) {
                task.setCompleted(true);
                taskRepository.save(task);
                sendTextMessage(chatId, "Завдання з ID " + taskId + " позначено як виконане.");
            } else {
                sendTextMessage(chatId, "Завдання з ID " + taskId + " вже було позначено як виконане раніше.");
            }
        } else {
            sendTextMessage(chatId, "Завдання з ID " + taskId + " не знайдено.");
        }
    }

    private void listExpiredTasks(long chatId) {
        List<Task> tasks = taskRepository.findByChatId(chatId);
        StringBuilder response = new StringBuilder("Протерміновані завдання:\n");
        int i = 1;
        for (Task task : tasks) {
            if (!task.isCompleted() && task.getDueDate().before(new Timestamp(System.currentTimeMillis()))) {
                response.append(i).append(") ID завдання: ").append(task.getId()).append("\n")
                        .append("Тема: ").append(task.getTitle()).append("\n")
                        .append("Опис: ").append(task.getDescription()).append("\n")
                        .append("Дедлайн: ").append(task.getDueDate()).append("\n\n");
                i++;
            }
        }
        if (i == 1) {
            response.append("У вас немає протермінованих завдань.");
        }
        sendTextMessage(chatId, response.toString());
    }

    private void listTaskPriority(long chatId) {
        List<Task> tasks = taskRepository.findByChatIdOrderByDueDateAsc(chatId);
        StringBuilder response = new StringBuilder("Пріоритетність завдань:\n");
        int i = 1;
        for (Task task : tasks) {
            response.append(i).append(") ID завдання: ").append(task.getId()).append("\n")
                    .append("Тема: ").append(task.getTitle()).append("\n")
                    .append("Опис: ").append(task.getDescription()).append("\n")
                    .append("Дедлайн: ").append(task.getDueDate()).append("\n\n");
            i++;
        }
        if (i == 1) {
            response.append("У вас немає завдань.");
        }
        sendTextMessage(chatId, response.toString());
    }

    private void listTaskTopicsAndDeadlines(long chatId) {
        List<Task> tasks = taskRepository.findByChatIdOrderByDueDateAsc(chatId);
        StringBuilder response = new StringBuilder("Теми завдань та їх терміни здачі (в порядку зростання дедлайну):\n");
        int i = 1;
        for (Task task : tasks) {
            response.append(i).append(") Тема: ").append(task.getTitle()).append("\n")
                    .append("Дедлайн: ").append(task.getDueDate()).append("\n\n");
            i++;
        }
        if (i == 1) {
            response.append("У вас немає завдань.");
        }
        sendTextMessage(chatId, response.toString());
    }

    private void sendStartMessage(long chatId, String firstName) {
        String messageText = "Привіт, " + firstName + ", раді бачити тебе!";
        log.info("Відповідь користувачу " + firstName);
        sendTextMessage(chatId, messageText);
    }

    private ReplyKeyboardMarkup createKeyboardMarkup() {
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
        return keyboardMarkup;
    }

    void sendTextMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        ReplyKeyboardMarkup keyboardMarkup = createKeyboardMarkup();
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Виникла помилка: " + e.getMessage());
        }
    }

    private void sendCustomKeyboard(long chatId, List<String> buttonValues) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Виберіть опцію:");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true); // Наступна клавіатура буде тимчасовою

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        for (String value : buttonValues) {
            row.add(value);
            // Якщо рядок досягнув максимальної довжини (2 кнопки в рядку), додати його до клавіатури і створити новий рядок
            if (row.size() >= 1) {
                keyboard.add(row);
                row = new KeyboardRow();
            }
        }

        // Додати останній рядок, якщо він не був доданий до клавіатури
        if (!row.isEmpty()) {
            keyboard.add(row);
        }

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Помилка відправки повідомлення: " + e.getMessage());
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
