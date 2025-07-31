package com.dophuong.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import com.dophuong.model.Product;
import com.dophuong.repository.ProductRepository;
import com.dophuong.service.ProductService;


@Service
public class ProductServiceImpl implements ProductService{
	
	
	@Autowired
	private ProductRepository productRepository;
	

	@Override
	public Product saveProduct(Product product) {
		return productRepository.save(product);
	}

	@Override
	public List<Product> getAllProducts() {
		return productRepository.findAll();
	}

	@Override
	public Boolean deleteProduct(Integer id) {
		Product product = productRepository.findById(id).orElse(null);

		if (product != null) {
			// ✅ Xóa ảnh nếu không phải ảnh mặc định
			if (product.getImage() != null && !product.getImage().equals("default.jpg")) {
				try {
					String uploadDir = System.getProperty("user.dir") + "/uploads/img/product_img";
					Path imagePath = Paths.get(uploadDir, product.getImage());

					if (Files.exists(imagePath)) {
						Files.delete(imagePath);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			productRepository.delete(product);
			return true;
		}

		return false;
	}


	@Override
	public Product getProductById(Integer id) {
		Product product = productRepository.findById(id).orElse(null);
		return product;
	}

	@Override
	public Product updateProduct(Product product, MultipartFile image) {
		Product dbProduct = getProductById(product.getId());

		String imageName = dbProduct.getImage();

		// Nếu có ảnh mới, đặt lại tên ảnh
		if (image != null && !image.isEmpty()) {
			String originalFilename = image.getOriginalFilename();
			String extension = originalFilename.substring(originalFilename.lastIndexOf(".")); // lấy đuôi file
			imageName = "product_" + System.currentTimeMillis() + extension;
		}

		// Cập nhật thông tin
		dbProduct.setTitle(product.getTitle());
		dbProduct.setDescription(product.getDescription());
		dbProduct.setCategory(product.getCategory());
		dbProduct.setPrice(product.getPrice());
		dbProduct.setStock(product.getStock());
		dbProduct.setImage(imageName);
		dbProduct.setIsActive(product.getIsActive());
		dbProduct.setDiscount(product.getDiscount());

		// Tính giá sau khi giảm
		double discount = product.getPrice() * (product.getDiscount() / 100.0);
		double discountPrice = product.getPrice() - discount;
		dbProduct.setDiscountPrice(discountPrice);

		Product updatedProduct = productRepository.save(dbProduct);

		// Lưu ảnh mới nếu có
		if (updatedProduct != null && image != null && !image.isEmpty()) {
			try {
				String uploadDir = System.getProperty("user.dir") + "/uploads/img/product_img";
				File uploadPath = new File(uploadDir);
				if (!uploadPath.exists()) uploadPath.mkdirs();

				Path path = Paths.get(uploadDir, imageName);
				Files.copy(image.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return updatedProduct;
	}

	@Override
	public List<Product> getAllActiveProducts(String category) {
		List<Product> products = null;
		if (ObjectUtils.isEmpty(category)) {
			products = productRepository.findByIsActiveTrue();
		}else {
			products=productRepository.findByCategory(category);
		}

		return products;
	}
	
	@Override
	public List<Product> searchProduct(String ch) {
		return productRepository.findByTitleContainingIgnoreCaseOrCategoryContainingIgnoreCase(ch, ch);
	}
	
	@Override
	public Page<Product> searchProductPagination(Integer pageNo, Integer pageSize, String ch) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		return productRepository.findByTitleContainingIgnoreCaseOrCategoryContainingIgnoreCase(ch, ch, pageable);
	}

	@Override
	public Page<Product> getAllActiveProductPagination(Integer pageNo, Integer pageSize, String category) {

		Pageable pageable = PageRequest.of(pageNo, pageSize);
		Page<Product> pageProduct = null;

		if (ObjectUtils.isEmpty(category)) {
			pageProduct = productRepository.findByIsActiveTrue(pageable);
		} else {
			pageProduct = productRepository.findByCategory(pageable, category);
		}
		return pageProduct;
	}
	
	@Override
	public Page<Product> getAllProductsPagination(Integer pageNo, Integer pageSize) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		return productRepository.findAll(pageable);
	}
	
	@Override
	public Page<Product> searchActiveProductPagination(Integer pageNo, Integer pageSize, String category, String ch) {

		Page<Product> pageProduct = null;
		Pageable pageable = PageRequest.of(pageNo, pageSize);

		pageProduct = productRepository.findByisActiveTrueAndTitleContainingIgnoreCaseOrCategoryContainingIgnoreCase(ch,
				ch, pageable);

//		if (ObjectUtils.isEmpty(category)) {
//			pageProduct = productRepository.findByIsActiveTrue(pageable);
//		} else {
//			pageProduct = productRepository.findByCategory(pageable, category);
//		}
		return pageProduct;
	}

}
