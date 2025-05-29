package com.dophuong.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.dophuong.model.Category;
import com.dophuong.model.Product;
import com.dophuong.service.CategoryService;
import com.dophuong.service.ProductService;

@Controller
public class HomeController {
	
	@Autowired
	private CategoryService categoryService;

	@Autowired
	private ProductService productService;

	@GetMapping("/")
	public String index() {
		return "index";
	}
	
	@GetMapping("/login")
	public String login() {
		return "login";
	}
	
	@GetMapping("/register")
	public String register() {
		return "register";
	}
	
	@GetMapping("/products")
	public String products(Model m, @RequestParam(value = "category", defaultValue = "") String category) {
		// System.out.println("category="+category);
		List<Category> categories = categoryService.getAllActiveCategory();
		List<Product> products = productService.getAllActiveProducts(category);
		m.addAttribute("categories", categories);
		m.addAttribute("products", products);
		m.addAttribute("paramValue", category);
		return "product";
	}
	
	@GetMapping("/product")
	public String product() {
		return "view_product";
	}
}
