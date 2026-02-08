package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "virtual_users")
@Data
public class VirtualUser {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ドメインテーブルと紐付けます
    @ManyToOne
    @JoinColumn(name = "domain_id", nullable = false)
    private VirtualDomain virtualDomain;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "maildir", nullable = false)
    private String maildir;
}
