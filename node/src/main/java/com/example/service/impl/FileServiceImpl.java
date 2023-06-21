package com.example.service.impl;

import com.example.dao.AppDocumentDAO;
import com.example.dao.AppPhotoDAO;
import com.example.dao.BinaryContentDAO;
import com.example.entity.AppDocument;
import com.example.entity.AppPhoto;
import com.example.entity.BinaryContent;
import com.example.exceptions.UploadFileException;
import com.example.service.FileService;
import com.example.service.enums.LinkType;
import com.example.utils.CryptoTool;
import lombok.extern.log4j.Log4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

@Service
@Log4j
//сервис получает телеграммовский месседж, выполнит все необх. действия
//для скачивания файла и сохранит его в БД
public class FileServiceImpl implements FileService {
    @Value("${token}")
    private String token;
    @Value("${service.file_info.uri}")

    private String fileInfoUri;
    @Value("${service.file_storage.uri}")

    private String fileStorageUri;
    @Value("${link.address}")
    private String linkAddress;
    private final AppDocumentDAO appDocumentDAO;
    private final AppPhotoDAO appPhotoDAO;
    private final BinaryContentDAO binaryContentDAO;
    private final CryptoTool cryptoTool;

    public FileServiceImpl(AppDocumentDAO appDocumentDAO, AppPhotoDAO appPhotoDAO, BinaryContentDAO binaryContentDAO, CryptoTool cryptoTool) {
        this.appDocumentDAO = appDocumentDAO;
        this.appPhotoDAO = appPhotoDAO;
        this.binaryContentDAO = binaryContentDAO;
        this.cryptoTool = cryptoTool;
    }

    @Override
    public AppDocument processDoc(Message telegramMessage) {
        Document telegramDoc = telegramMessage.getDocument();
        String fileId = telegramDoc.getFileId();
        ResponseEntity<String> response = getFilePath(fileId);
        if (response.getStatusCode() == HttpStatus.OK) {
            BinaryContent persistentBinaryContent = getPersistentBinaryContent(response);
            AppDocument transientAppDoc = buildTransientAppDoc(telegramDoc, persistentBinaryContent); //генерим свою сущность Document c этим только что сохраненным в нее объектом binaryContent
            return appDocumentDAO.save(transientAppDoc); // сохраняем в БД и возвращаем из метода persistentObject
        } else {
            throw new UploadFileException("Bad response from telegram service: " + response);
        }
    }

    private BinaryContent getPersistentBinaryContent(ResponseEntity<String> response) {
        String filePath = getFilePath(response);
        byte[] fileByte = downloadFile(filePath);
        BinaryContent transientBinaryContent = BinaryContent.builder()
                .fileAsArrayOfBytes(fileByte)
                .build();
        return binaryContentDAO.save(transientBinaryContent);
    }

    private String getFilePath(ResponseEntity<String> response) {
        JSONObject jsonObject = new JSONObject(response.getBody());
        return String.valueOf(jsonObject
                .getJSONObject("result")
                .getString("file_path"));
    }

    @Override
    public AppPhoto processPhoto(Message telegramMessage) {
        var photoSizeCount = telegramMessage.getPhoto().size();
        var photoIndex = photoSizeCount > 1 ? telegramMessage.getPhoto().size() - 1 : 0;
        PhotoSize telegramPhoto = telegramMessage.getPhoto().get(photoIndex);
        String fileId = telegramPhoto.getFileId();
        ResponseEntity<String> response = getFilePath(fileId);
        if (response.getStatusCode() == HttpStatus.OK) {
            BinaryContent persistentBinaryContent = getPersistentBinaryContent(response);
            AppPhoto transientAppDoc = buildTransientAppPhoto(telegramPhoto, persistentBinaryContent); //генерим свою сущность Document c этим только что сохраненным в нее объектом binaryContent
            return appPhotoDAO.save(transientAppDoc); // сохраняем в БД и возвращаем из метода persistentObject
        } else {
            throw new UploadFileException("Bad response from telegram service: " + response);
        }
    }

    //метод достает значения из полей телеграмовского объекта докуиента и уст-т их в наш объект
    private AppDocument buildTransientAppDoc(Document telegramDoc, BinaryContent persistentBinaryContent) {
        return AppDocument.builder()
                .telegramFileId(telegramDoc.getFileId())
                .docName(telegramDoc.getFileName())
                .binaryContent(persistentBinaryContent)
                .mimeType(telegramDoc.getMimeType())
                .fileSize(telegramDoc.getFileSize())
                .build();
    }

    private AppPhoto buildTransientAppPhoto(PhotoSize telegramPhoto, BinaryContent persistentBinaryContent) {
        return AppPhoto.builder()
                .telegramFileId(telegramPhoto.getFileId())
                .binaryContent(persistentBinaryContent)
                .fileSize(telegramPhoto.getFileSize())
                .build();
    }

    //позволяет сделать http-запрос к телеграму
    private ResponseEntity<String> getFilePath(String fileId) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> request = new HttpEntity<>(headers);

        return restTemplate.exchange(
                fileInfoUri,
                HttpMethod.GET,
                request,
                String.class,
                token, fileId
        );
    }

    //формируем окончательный uri
    private byte[] downloadFile(String filePath) {
        String fulUri = fileStorageUri.replace("{token}", token)
                .replace("{filePath}", filePath);
        URL urlObj = null;
        try {
            urlObj = new URL(fulUri);
        } catch (MalformedURLException e) {
            throw new UploadFileException(urlObj.toExternalForm(), e);
        }

        //TODO подумать над оптимизацией
        try (InputStream is = urlObj.openStream()) { //открываеи стрим для скаяивания контента
            return is.readAllBytes();
        } catch (IOException e) {
            throw new UploadFileException(urlObj.toExternalForm(), e);
        }
    }

    @Override
    public String generateLink(Long docId, LinkType linkType) {
        var hash = cryptoTool.hashOf(docId);
        return "http://" + linkAddress + "/" + linkType + "?id=" + hash;
    }
}
