package com.example.minsumgr.domain;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role; // USER / ADMIN

    private String phone;

    private String email;
    
    private String realName; // 真实姓名
    
    private String idCard; // 身份证号
    
    private String address; // 地址
    
    private String avatar; // 头像URL
}
