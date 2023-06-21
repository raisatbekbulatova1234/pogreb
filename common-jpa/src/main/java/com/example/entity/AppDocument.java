package com.example.entity;

import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@EqualsAndHashCode(exclude = "id")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "app_document")
public class AppDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String telegramFileId;
    private String docName;
    @OneToOne //одной записи BinaryContent соответствуе одна запись AppDocument
    private BinaryContent binaryContent; //внешний ключ
    private String mimeType;
    private Long fileSize;
}
