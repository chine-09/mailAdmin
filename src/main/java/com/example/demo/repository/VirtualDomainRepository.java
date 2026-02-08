package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.VirtualDomain;
public interface VirtualDomainRepository extends JpaRepository<VirtualDomain, Integer>{
	
	VirtualDomain findByName(String name);
}
