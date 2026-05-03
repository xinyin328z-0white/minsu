package com.example.minsumgr.domain;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "routes")
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    private String tags; // comma separated
}
