package com.dophuong.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.dophuong.model.Cart;
import com.dophuong.model.Category;
import com.dophuong.model.OrderRequest;
import com.dophuong.model.ProductOrder;
import com.dophuong.model.UserDtls;
import com.dophuong.service.CartService;
import com.dophuong.service.CategoryService;
import com.dophuong.service.OrderService;
import com.dophuong.service.UserService;
import com.dophuong.util.CommonUtil;
import com.dophuong.util.OrderStatus;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserService userService;

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private CartService cartService;

	@Autowired
	private OrderService orderService;

	@Autowired
	private CommonUtil commonUtil;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@GetMapping("/")

	public String home() {
		return "user/home";
	}

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

	@GetMapping("/addCart")
	public String addToCart(@RequestParam Integer pid, @RequestParam Integer uid, HttpSession session) {
		Cart saveCart = cartService.saveCart(pid, uid);

		if (ObjectUtils.isEmpty(saveCart)) {
			session.setAttribute("errorMsg", "Thêm sản phẩm vào giỏ hàng thất bại");
		} else {
			session.setAttribute("succMsg", "Đã thêm sản phẩm vào giỏ hàng");
		}
		return "redirect:/product/" + pid;
	}

	@GetMapping("/cart")
	public String loadCartPage(Principal p, Model m) {

		UserDtls user = getLoggedInUserDetails(p);
		List<Cart> carts = cartService.getCartsByUser(user.getId());
		m.addAttribute("carts", carts);
		if (carts.size() > 0) {
			Double totalOrderPrice = carts.get(carts.size() - 1).getTotalOrderPrice();
			m.addAttribute("totalOrderPrice", totalOrderPrice);
		}
		return "/user/cart";
	}

	@GetMapping("/cartQuantityUpdate")
	public String updateCartQuantity(@RequestParam String sy, @RequestParam Integer cid) {
		cartService.updateQuantity(sy, cid);
		return "redirect:/user/cart";
	}

	private UserDtls getLoggedInUserDetails(Principal p) {
		String email = p.getName();
		UserDtls userDtls = userService.getUserByEmail(email);
		return userDtls;
	}

	@GetMapping("/orders")
	public String orderPage(Principal p, Model m) {
		UserDtls user = getLoggedInUserDetails(p);
		List<Cart> carts = cartService.getCartsByUser(user.getId());
		m.addAttribute("carts", carts);
		if (carts.size() > 0) {
			Double orderPrice = carts.get(carts.size() - 1).getTotalOrderPrice();
			Double totalOrderPrice = carts.get(carts.size() - 1).getTotalOrderPrice() + 250 + 100;
			m.addAttribute("orderPrice", orderPrice);
			m.addAttribute("totalOrderPrice", totalOrderPrice);
		}
		return "/user/order";
	}

	@PostMapping("/save-order")
	public String saveOrder(@ModelAttribute OrderRequest request, Principal p) throws Exception {
		// System.out.println(request);
		UserDtls user = getLoggedInUserDetails(p);
		orderService.saveOrder(user.getId(), request);

		return "redirect:/user/success";
	}

	@GetMapping("/success")
	public String loadSuccess() {
		return "/user/success";
	}

	@GetMapping("/user-orders")
	public String myOrder(Model m, Principal p) {
		UserDtls loginUser = getLoggedInUserDetails(p);
		List<ProductOrder> orders = orderService.getOrdersByUser(loginUser.getId());
		m.addAttribute("orders", orders);
		return "/user/my_orders";
	}

	@GetMapping("/update-status")
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
			session.setAttribute("succMsg", "Cập nhật trạng thái thành công");
		} else {
			session.setAttribute("errorMsg", "Cập nhật trạng thái thất bại");
		}
		return "redirect:/user/user-orders";
	}

	@GetMapping("/profile")
	public String profile() {
		return "/user/profile";
	}

	@PostMapping("/update-profile")
	public String updateProfile(@ModelAttribute UserDtls user, @RequestParam MultipartFile img, HttpSession session) {
		UserDtls updateUserProfile = userService.updateUserProfile(user, img);
		if (ObjectUtils.isEmpty(updateUserProfile)) {
			session.setAttribute("errorMsg", "Cập nhật hồ sơ thất bại");
		} else {
			session.setAttribute("succMsg", "Cập nhật hồ sơ thành công");
		}
		return "redirect:/user/profile";
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

		return "redirect:/user/profile";
	}

}
