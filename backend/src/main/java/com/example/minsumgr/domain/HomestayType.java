package com.example.minsumgr.domain;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "homestay_types")
public class HomestayType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;
}
