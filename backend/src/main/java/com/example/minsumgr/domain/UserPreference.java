package com.example.minsumgr.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "user_preferences",
        uniqueConstraints = @UniqueConstraint(columnNames = "userId")
)
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    private String budgetLevel; // ECONOMY / MODERATE / LUXURY

    @Column(columnDefinition = "TEXT")
    private String travelStyles; // comma separated

    @Column(columnDefinition = "TEXT")
    private String preferredLocations; // comma separated

    private Integer minPrice;

    private Integer maxPrice;

    private String companionType;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
