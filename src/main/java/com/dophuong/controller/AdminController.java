package com.dophuong.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;

import com.dophuong.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.dophuong.model.Category;
import com.dophuong.model.Product;
import com.dophuong.model.ProductOrder;
import com.dophuong.model.UserDtls;
import com.dophuong.util.CommonUtil;
import com.dophuong.util.OrderStatus;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private ProductService productService;

	@Autowired
	private UserService userService;

	@Autowired
	private CartService cartService;

	@Autowired
	private OrderService orderService;

	@Autowired
	private CommonUtil commonUtil;
	
	@Autowired
	private PasswordEncoder passwordEncoder;

	@ModelAttribute
	public void getUserDetails(Principal p, Model m) {
		if (p != null) {
			String email = p.getName();
			UserDtls userDtls = userService.getUserByEmail(email);
			m.addAttribute("user", userDtls);
			Integer countCart = cartService.getCountCart(userDtls.getId());
			m.addAttribute("countCart", countCart);
		}

		List<Category> allActiveCategory = categoryService.getAllActiveCategory();
		m.addAttribute("categorys", allActiveCategory);
	}

	@GetMapping("/")
	public String index() {
		return "admin/index";
	}

	@GetMapping("/loadAddProduct")
	public String loadAddProduct(Model m) {
		List<Category> categories = categoryService.getAllCategory();
		m.addAttribute("categories", categories);
		return "admin/add_product";
	}

	@GetMapping("/category")
	public String category(Model m, @RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "5") Integer pageSize) {
		// m.addAttribute("categorys", categoryService.getAllCategory());
		Page<Category> page = categoryService.getAllCategorPagination(pageNo, pageSize);
		List<Category> categorys = page.getContent();
		m.addAttribute("categorys", categorys);

		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());

		return "admin/category";
	}

	@PostMapping("/saveCategory")
	public String saveCategory(@ModelAttribute Category category, @RequestParam("file") MultipartFile file,
							   HttpSession session) throws IOException {

		String imageName = "default.jpg";

		// Nếu người dùng có upload file thì đổi tên
		if (file != null && !file.isEmpty()) {
			String originalFilename = file.getOriginalFilename();
			String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".")); // lấy đuôi .jpg/.png

			// Tạo tên ảnh mới theo timestamp
			imageName = "category_" + System.currentTimeMillis() + fileExtension;
		}

		category.setImageName(imageName);

		Boolean existCategory = categoryService.existCategory(category.getName());

		if (existCategory) {
			session.setAttribute("errorMsg", "Tên danh mục đã tồn tại");
		} else {
			Category saveCategory = categoryService.saveCategory(category);

			if (ObjectUtils.isEmpty(saveCategory)) {
				session.setAttribute("errorMsg", "Lưu không thành công! Lỗi hệ thống");
			} else {
				String uploadDir = System.getProperty("user.dir") + "/uploads/img/category_img";
				File uploadPath = new File(uploadDir);
				if (!uploadPath.exists()) uploadPath.mkdirs();

				if (file != null && !file.isEmpty()) {
					Path path = Paths.get(uploadDir, imageName);
					Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				}

				session.setAttribute("succMsg", "Lưu danh mục thành công");
			}
		}

		return "redirect:/admin/category";
	}

	@GetMapping("/deleteCategory/{id}")
	public String deleteCategory(@PathVariable int id, HttpSession session) {
		Boolean deleteCategory = categoryService.deleteCategory(id);

		if (deleteCategory) {
			session.setAttribute("succMsg", "Xóa danh mục thành công.");
		} else {
			session.setAttribute("errorMsg", "Đã xảy ra lỗi trên máy chủ. Vui lòng thử lại sau.");
		}
		return "redirect:/admin/category";
	}

	@GetMapping("/loadEditCategory/{id}")
	public String loadEditCategory(@PathVariable int id, Model m) {
		m.addAttribute("category", categoryService.getCategoryById(id));
		return "admin/edit_category";
	}

	@PostMapping("/updateCategory")
	public String updateCategory(@ModelAttribute Category category, @RequestParam("file") MultipartFile file,
								 HttpSession session) throws IOException {

		// Lấy dữ liệu cũ từ DB
		Category oldCategory = categoryService.getCategoryById(category.getId());
		if (oldCategory == null) {
			session.setAttribute("errorMsg", "Danh mục không tồn tại");
			return "redirect:/admin/loadEditCategory/" + category.getId();
		}

		// Đặt lại tên ảnh nếu có file mới
		String imageName = oldCategory.getImageName(); // giữ nguyên nếu không upload mới
		if (file != null && !file.isEmpty()) {
			String originalFilename = file.getOriginalFilename();
			String extension = originalFilename.substring(originalFilename.lastIndexOf(".")); // .jpg, .png,...
			imageName = "category_" + System.currentTimeMillis() + extension;
		}

		// Cập nhật dữ liệu danh mục
		oldCategory.setName(category.getName());
		oldCategory.setIsActive(category.getIsActive());
		oldCategory.setImageName(imageName);

		Category updateCategory = categoryService.saveCategory(oldCategory);

		if (!ObjectUtils.isEmpty(updateCategory)) {

			// Nếu có upload ảnh mới → lưu ảnh
			if (file != null && !file.isEmpty()) {
				String uploadDir = System.getProperty("user.dir") + "/uploads/img/category_img";
				File uploadPath = new File(uploadDir);
				if (!uploadPath.exists()) uploadPath.mkdirs();

				Path path = Paths.get(uploadDir, imageName);
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}

			session.setAttribute("succMsg", "Cập nhật danh mục thành công");
		} else {
			session.setAttribute("errorMsg", "Lỗi hệ thống! Không thể cập nhật danh mục");
		}

		return "redirect:/admin/loadEditCategory/" + category.getId();
	}

	@PostMapping("/saveProduct")
	public String saveProduct(@ModelAttribute Product product, @RequestParam("file") MultipartFile image,
							  HttpSession session) throws IOException {

		String imageName = "default.jpg";

		// Nếu có upload ảnh → đổi tên theo thời gian
		if (image != null && !image.isEmpty()) {
			String originalFilename = image.getOriginalFilename();
			String extension = originalFilename.substring(originalFilename.lastIndexOf(".")); // .jpg, .png,...
			imageName = "product_" + System.currentTimeMillis() + extension;
		}

		// Thiết lập thông tin sản phẩm
		product.setImage(imageName);
		product.setDiscount(0);
		product.setDiscountPrice(product.getPrice());

		Product savedProduct = productService.saveProduct(product);

		if (!ObjectUtils.isEmpty(savedProduct)) {

			// Nếu có file ảnh mới thì lưu vào thư mục uploads
			if (image != null && !image.isEmpty()) {
				String uploadDir = System.getProperty("user.dir") + "/uploads/img/product_img";
				File uploadPath = new File(uploadDir);
				if (!uploadPath.exists()) uploadPath.mkdirs();

				Path path = Paths.get(uploadDir, imageName);
				Files.copy(image.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}

			session.setAttribute("succMsg", "Thêm sản phẩm thành công");
		} else {
			session.setAttribute("errorMsg", "Lỗi hệ thống! Không thể lưu sản phẩm");
		}

		return "redirect:/admin/loadAddProduct";
	}


	@GetMapping("/products")
	public String loadViewProduct(Model m, @RequestParam(defaultValue = "") String ch,
			@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "5") Integer pageSize) {

//		List<Product> products = null;
//		if (ch != null && ch.length() > 0) {
//			products = productService.searchProduct(ch);
//		} else {
//			products = productService.getAllProducts();
//		}
//		m.addAttribute("products", products);

		Page<Product> page = null;
		if (ch != null && ch.length() > 0) {
			page = productService.searchProductPagination(pageNo, pageSize, ch);
		} else {
			page = productService.getAllProductsPagination(pageNo, pageSize);
		}
		m.addAttribute("products", page.getContent());

		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());

		return "admin/products";
	}

	@GetMapping("/deleteProduct/{id}")
	public String deleteProduct(@PathVariable int id, HttpSession session) {
		Boolean deleteProduct = productService.deleteProduct(id);
		if (deleteProduct) {
			session.setAttribute("succMsg", "Xóa sản phẩm thành công.");
		} else {
			session.setAttribute("errorMsg", "Đã xảy ra lỗi trên máy chủ. Vui lòng thử lại sau.");
		}
		return "redirect:/admin/products";
	}

	@GetMapping("/editProduct/{id}")
	public String editProduct(@PathVariable int id, Model m) {
		m.addAttribute("product", productService.getProductById(id));
		m.addAttribute("categories", categoryService.getAllCategory());
		return "admin/edit_product";
	}

	@PostMapping("/updateProduct")
	public String updateProduct(@ModelAttribute Product product, @RequestParam("file") MultipartFile image,
			HttpSession session, Model m) {

		Product updateProduct = productService.updateProduct(product, image);
		if (!ObjectUtils.isEmpty(updateProduct)) {
			session.setAttribute("succMsg", "Cập nhật sản phẩm thành công.");
		} else {
			session.setAttribute("errorMsg", "Đã xảy ra lỗi trên máy chủ. Vui lòng thử lại sau.");
		}

		return "redirect:/admin/editProduct/" + product.getId();
	}

	@GetMapping("/users")
	public String getAllUsers(Model m, @RequestParam Integer type) {
		List<UserDtls> users = null;
		if (type == 1) {
			users = userService.getUsers("ROLE_USER");
		} else {
			users = userService.getUsers("ROLE_ADMIN");
		}
		m.addAttribute("userType",type);
		m.addAttribute("users", users);
		return "/admin/users";
	}

	@GetMapping("/updateSts")
	public String updateUserAccountStatus(@RequestParam Boolean status, @RequestParam Integer id,@RequestParam Integer type, HttpSession session) {
		Boolean f = userService.updateAccountStatus(id, status);
		if (f) {
			session.setAttribute("succMsg", "Cập nhật trạng thái tài khoản thành công.");
		} else {
			session.setAttribute("errorMsg", "Đã xảy ra lỗi trên máy chủ. Vui lòng thử lại sau.");
		}
		return "redirect:/admin/users?type="+type;
	}

	@GetMapping("/orders")
	public String getAllOrders(Model m, @RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "5") Integer pageSize) {
//		List<ProductOrder> allOrders = orderService.getAllOrders();
//		m.addAttribute("orders", allOrders);
//		m.addAttribute("srch", false);

		Page<ProductOrder> page = orderService.getAllOrdersPagination(pageNo, pageSize);
		m.addAttribute("orders", page.getContent());
		m.addAttribute("srch", false);

		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());

		return "/admin/orders";
	}

	@PostMapping("/update-order-status")
	public String updateOrderStatus(@RequestParam Integer id, @RequestParam Integer st, HttpSession session) {

		OrderStatus[] values = OrderStatus.values();
		String status = null;

		for (OrderStatus orderSt : values) {
			if (orderSt.getId().equals(st)) {
				status = orderSt.getName();
			}
		}

		ProductOrder updateOrder = orderService.updateOrderStatus(id, status);

		try {
			commonUtil.sendMailForProductOrder(updateOrder, status);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (!ObjectUtils.isEmpty(updateOrder)) {
			session.setAttribute("succMsg", "Cập nhật trạng thái thành công.");
		} else {
			session.setAttribute("errorMsg", "Không thể cập nhật trạng thái.");
		}
		return "redirect:/admin/orders";
	}

	@GetMapping("/search-order")
	public String searchProduct(@RequestParam String orderId, Model m, HttpSession session) {

		if (orderId != null && orderId.length() > 0) {

			ProductOrder order = orderService.getOrdersByOrderId(orderId.trim());

			if (ObjectUtils.isEmpty(order)) {
				session.setAttribute("errorMsg", "Mã đơn hàng không chính xác.");
				m.addAttribute("orderDtls", null);
			} else {
				m.addAttribute("orderDtls", order);
			}

			m.addAttribute("srch", true);
		} else {
			List<ProductOrder> allOrders = orderService.getAllOrders();
			m.addAttribute("orders", allOrders);
			m.addAttribute("srch", false);
		}
		return "/admin/orders";

	}

	@GetMapping("/add-admin")
	public String loadAdminAdd() {
		return "/admin/add_admin";
	}

	@PostMapping("/save-admin")
	public String saveAdmin(@ModelAttribute UserDtls user, @RequestParam("img") MultipartFile file, HttpSession session)
			throws IOException {

		String imageName = "default.jpg";

		// Nếu có ảnh được upload, đặt lại tên file theo timestamp
		if (file != null && !file.isEmpty()) {
			String originalFilename = file.getOriginalFilename();
			String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
			imageName = "admin_" + System.currentTimeMillis() + extension;
		}

		user.setProfileImage(imageName);
		UserDtls saveUser = userService.saveAdmin(user);

		if (!ObjectUtils.isEmpty(saveUser)) {
			if (file != null && !file.isEmpty()) {
				try {
					String uploadDir = System.getProperty("user.dir") + "/uploads/img/profile_img";
					File uploadPath = new File(uploadDir);
					if (!uploadPath.exists()) uploadPath.mkdirs();

					Path path = Paths.get(uploadDir, imageName);
					Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					e.printStackTrace();
					session.setAttribute("errorMsg", "Không thể lưu ảnh. Vui lòng thử lại.");
					return "redirect:/admin/add-admin";
				}
			}

			session.setAttribute("succMsg", "Thêm quản trị viên thành công.");
		} else {
			session.setAttribute("errorMsg", "Đã xảy ra lỗi trên máy chủ. Vui lòng thử lại sau.");
		}

		return "redirect:/admin/add-admin";
	}


	@GetMapping("/profile")
	public String profile() {
		return "/admin/profile";
	}

	@PostMapping("/update-profile")
	public String updateProfile(@ModelAttribute UserDtls user, @RequestParam MultipartFile img, HttpSession session) {
		UserDtls updateUserProfile = userService.updateUserProfile(user, img);
		if (ObjectUtils.isEmpty(updateUserProfile)) {
			session.setAttribute("errorMsg", "Cập nhật hồ sơ thất bại.");
		} else {
			session.setAttribute("succMsg", "Cập nhật hồ sơ thành công.");
		}
		return "redirect:/admin/profile";
	}
	
	private UserDtls getLoggedInUserDetails(Principal p) {
		String email = p.getName();
		UserDtls userDtls = userService.getUserByEmail(email);
		return userDtls;
	}

	@PostMapping("/change-password")
	public String changePassword(@RequestParam String newPassword, @RequestParam String confirmPassword,
			@RequestParam String currentPassword, Principal principal, HttpSession session) {

		UserDtls loggedInUser = getLoggedInUserDetails(principal);

		// 1. Kiểm tra mật khẩu hiện tại
		if (!passwordEncoder.matches(currentPassword, loggedInUser.getPassword())) {
			session.setAttribute("errorMsg", "Mật khẩu hiện tại không chính xác.");
			session.removeAttribute("succMsg");
			return "redirect:/user/profile";
		}

		// 2. Kiểm tra mật khẩu mới không rỗng
		if (newPassword == null || newPassword.trim().isEmpty() || confirmPassword == null
				|| confirmPassword.trim().isEmpty()) {
			session.setAttribute("errorMsg", "Vui lòng nhập đầy đủ mật khẩu mới và xác nhận.");
			session.removeAttribute("succMsg");
			return "redirect:/user/profile";
		}

		// 3. Kiểm tra mật khẩu khớp
		if (!newPassword.equals(confirmPassword)) {
			session.setAttribute("errorMsg", "Mật khẩu mới và mật khẩu xác nhận không khớp.");
			session.removeAttribute("succMsg");
			return "redirect:/user/profile"; // PHẢI return ngay để không ghi DB
		}

		// 4. Kiểm tra mật khẩu mới không trùng mật khẩu cũ
		if (passwordEncoder.matches(newPassword, loggedInUser.getPassword())) {
			session.setAttribute("errorMsg", "Mật khẩu mới không được trùng với mật khẩu hiện tại.");
			session.removeAttribute("succMsg");
			return "redirect:/user/profile";
		}

		// 5. Kiểm tra độ mạnh mật khẩu
		String strongPasswordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
		if (!newPassword.matches(strongPasswordRegex)) {
			session.setAttribute("errorMsg",
					"Mật khẩu mới phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.");
			session.removeAttribute("succMsg");
			return "redirect:/user/profile";
		}

		// 6. Mọi kiểm tra đã qua: cập nhật mật khẩu
		try {
			String encodedNewPassword = passwordEncoder.encode(newPassword);
			loggedInUser.setPassword(encodedNewPassword);
			UserDtls updatedUser = userService.updateUser(loggedInUser);

			if (updatedUser != null) {
				session.setAttribute("succMsg", "Đổi mật khẩu thành công.");
				session.removeAttribute("errorMsg");
			} else {
				session.setAttribute("errorMsg", "Lỗi hệ thống: Không thể cập nhật mật khẩu.");
				session.removeAttribute("succMsg");
			}
		} catch (Exception e) {
			session.setAttribute("errorMsg", "Đã xảy ra lỗi: " + e.getMessage());
			session.removeAttribute("succMsg");
		}

		return "redirect:/admin/profile";
	}

}
