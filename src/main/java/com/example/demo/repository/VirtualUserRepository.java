package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.VirtualUser;

public interface VirtualUserRepository extends JpaRepository<VirtualUser, Integer>{

}
