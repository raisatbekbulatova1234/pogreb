package com.example.service;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface ConsumerService { //для считывания сообщений из брокераб под каждую очередь в брокере свой метод
    void consumeTextMessageUpdates(Update update);
    void consumeDocMessageUpdates(Update update);
    void consumePhotoMessageUpdates(Update update);
}
