package com.example.mypolytechnik.model;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserFileRepository extends JpaRepository<UserFile, Long> {
    List<UserFile> findByChatId(Long chatId);
}