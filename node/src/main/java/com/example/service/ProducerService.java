package com.example.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public interface ProducerService { //нужен для отправки ответов с node в брокер
    void producerAnswer(SendMessage sendMessage);
}
