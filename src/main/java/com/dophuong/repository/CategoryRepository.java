package com.dophuong.repository;

import com.dophuong.model.Category;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer>{
	public Boolean existsByName(String name);
	
	public List<Category> findByIsActiveTrue();

}
