package com.dophuong.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.dophuong.model.Category;
import com.dophuong.repository.CategoryRepository;
import com.dophuong.service.CategoryService;

@Service
public class CategoryServiceImpl implements CategoryService {

	@Autowired
	private CategoryRepository categoryRepository;

	@Override
	public Category saveCategory(Category category) {

		return categoryRepository.save(category);
	}

	@Override
	public List<Category> getAllCategory() {

		return categoryRepository.findAll();
	}

	@Override
	public Boolean existCategory(String name) {

		return categoryRepository.existsByName(name);
	}

	@Override
	public Boolean deleteCategory(int id) {
		Category category = categoryRepository.findById(id).orElse(null);

		if (category != null) {
			if (category.getImageName() != null && !category.getImageName().equals("default.jpg")) {
				try {
					// Đường dẫn thư mục lưu ảnh ngoài: uploads/category_img
					String uploadDir = System.getProperty("user.dir") + "/uploads/img/category_img";
					Path imagePath = Paths.get(uploadDir, category.getImageName());

					if (Files.exists(imagePath)) {
						Files.delete(imagePath);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			categoryRepository.delete(category);
			return true;
		}

		return false;
	}



	@Override
	public Category getCategoryById(int id) {
		Category category = categoryRepository.findById(id).orElse(null);
		return category;
	}
	
	@Override
	public List<Category> getAllActiveCategory() {
		List<Category> categories = categoryRepository.findByIsActiveTrue();
		return categories;
	}
	
	@Override
	public Page<Category> getAllCategorPagination(Integer pageNo, Integer pageSize) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		return categoryRepository.findAll(pageable);
	}

}
