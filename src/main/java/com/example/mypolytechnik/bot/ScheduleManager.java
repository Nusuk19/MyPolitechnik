package com.example.mypolytechnik.bot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class ScheduleManager {

    private Map<DayOfWeek, String[]> scheduleMap;
    private Connection connection;
    private MyTelegramBot telegramBot;

    public ScheduleManager(MyTelegramBot telegramBot) {
        this.telegramBot = telegramBot;
        initializeDatabaseConnection();
        initializeSchedule();
    }

    private void initializeDatabaseConnection() {
        String url = "jdbc:mysql://localhost:3306/tg-bot";
        String username = "root";
        String password = "1324576809Aa";

        try {
            connection = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initializeSchedule() {
        scheduleMap = new HashMap<>();
        String query = "SELECT day_of_week, lesson1, lesson2, lesson3, lesson4, lesson5 FROM schedule";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                DayOfWeek dayOfWeek = DayOfWeek.valueOf(resultSet.getString("day_of_week"));
                String[] lessons = new String[]{
                        resultSet.getString("lesson1"),
                        resultSet.getString("lesson2"),
                        resultSet.getString("lesson3"),
                        resultSet.getString("lesson4"),
                        resultSet.getString("lesson5")
                };
                scheduleMap.put(dayOfWeek, lessons);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getScheduleForToday() {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        String[] lessons = scheduleMap.get(today);
        if (lessons != null) {
            StringBuilder schedule = new StringBuilder("Розклад на сьогодні (").append(today.toString()).append("):\n");
            for (int i = 0; i < lessons.length; i++) {
                schedule.append("Урок ").append(i + 1).append(": ").append(lessons[i]).append("\n");
            }
            return schedule.toString();
        } else {
            return "На жаль, розклад для сьогоднішнього дня не знайдено. Сьогодні вихідний.";
        }
    }

    public void handleScheduleCommand(long chatId) {
        String schedule = getScheduleForToday();
        telegramBot.sendTextMessage(chatId, schedule); // Викликаємо метод sendTextMessage з класу TelegramBot
    }
}