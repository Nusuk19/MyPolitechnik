package com.example.mypolytechnik.bot.task;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByChatId(Long chatId);

    List<Task> findByChatIdOrderByDueDateAsc(long chatId);
}