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
@Table(name = "app_photo")
public class AppPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String telegramFileId;
    @OneToOne //одной записи BinaryContent соответствуе одна запись AppDocument
    private BinaryContent binaryContent; //внешний ключ
    private Integer fileSize;
}
