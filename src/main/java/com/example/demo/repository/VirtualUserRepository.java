package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.VirtualUser;

public interface VirtualUserRepository extends JpaRepository<VirtualUser, Integer>{
	VirtualUser findByEmail(String email);
	List<VirtualUser> findByEmailContaining(String keyword);
}
