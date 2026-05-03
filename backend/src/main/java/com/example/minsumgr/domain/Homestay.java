package com.example.minsumgr.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "homestays")
public class Homestay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String location;

    private String description;

    private BigDecimal pricePerNight;

    private Long typeId;

    private Integer availableRooms;

    @Column(columnDefinition = "TEXT")
    private String roomNotes; // 房间注意事项

    @Column(columnDefinition = "TEXT")
    private String imageUrl; // 民宿图片URL
}
