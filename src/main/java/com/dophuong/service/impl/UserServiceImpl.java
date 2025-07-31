package com.dophuong.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import com.dophuong.model.UserDtls;
import com.dophuong.repository.UserRepository;
import com.dophuong.service.UserService;
import com.dophuong.util.AppConstant;


@Service
public class UserServiceImpl implements UserService{
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private PasswordEncoder passwordEncoder;

	@Override
	public UserDtls saveUser(UserDtls user) {
		user.setRole("ROLE_USER");
		user.setIsEnable(true);
		user.setAccountNonLocked(true);
		user.setFailedAttempt(0);

		String encodePassword = passwordEncoder.encode(user.getPassword());
		user.setPassword(encodePassword);
		UserDtls saveUser = userRepository.save(user);
		return saveUser;
	}

	@Override
	public UserDtls getUserByEmail(String email) {
		return userRepository.findByEmail(email);
	}

	@Override
	public List<UserDtls> getUsers(String role) {
		return userRepository.findByRole(role);
	}

	@Override
	public Boolean updateAccountStatus(Integer id, Boolean status) {
		Optional<UserDtls> findByuser = userRepository.findById(id);

		if (findByuser.isPresent()) {
			UserDtls userDtls = findByuser.get();
			userDtls.setIsEnable(status);
			userRepository.save(userDtls);
			return true;
		}

		return false;
	}

	@Override
	public void increaseFailedAttempt(UserDtls user) {
		int attempt = user.getFailedAttempt() + 1;
		user.setFailedAttempt(attempt);
		userRepository.save(user);
	}

	@Override
	public void userAccountLock(UserDtls user) {
		user.setAccountNonLocked(false);
		user.setLockTime(new Date());
		userRepository.save(user);
	}

	@Override
	public boolean unlockAccountTimeExpired(UserDtls user) {

		long lockTime = user.getLockTime().getTime();
		long unLockTime = lockTime + AppConstant.UNLOCK_DURATION_TIME;

		long currentTime = System.currentTimeMillis();

		if (unLockTime < currentTime) {
			user.setAccountNonLocked(true);
			user.setFailedAttempt(0);
			user.setLockTime(null);
			userRepository.save(user);
			return true;
		}

		return false;
	}

	@Override
	public void resetAttempt(int userId) {

	}

	@Override
	public void updateUserResetToken(String email, String resetToken) {
		UserDtls findByEmail = userRepository.findByEmail(email);
		findByEmail.setResetToken(resetToken);
		userRepository.save(findByEmail);
	}

	@Override
	public UserDtls getUserByToken(String token) {
		return userRepository.findByResetToken(token);
	}

	@Override
	public UserDtls updateUser(UserDtls user) {
		return userRepository.save(user);
	}

	@Override
	public UserDtls updateUserProfile(UserDtls user, MultipartFile img) {
		UserDtls dbUser = userRepository.findById(user.getId()).orElse(null);

		if (dbUser == null) {
			return null; // hoặc throw Exception nếu muốn
		}

		String imageName = dbUser.getProfileImage();

		// Nếu có ảnh mới thì tạo tên ảnh mới theo timestamp
		if (img != null && !img.isEmpty()) {
			String originalFilename = img.getOriginalFilename();
			String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
			imageName = "user_" + System.currentTimeMillis() + extension;
		}

		// Cập nhật thông tin từ form
		dbUser.setName(user.getName());
		dbUser.setMobileNumber(user.getMobileNumber());
		dbUser.setAddress(user.getAddress());
		dbUser.setCity(user.getCity());
		dbUser.setState(user.getState());
		dbUser.setPincode(user.getPincode());
		dbUser.setProfileImage(imageName);

		// Lưu thông tin người dùng trước
		UserDtls updatedUser = userRepository.save(dbUser);

		// Sau đó mới xử lý lưu ảnh
		if (img != null && !img.isEmpty()) {
			try {
				String uploadDir = System.getProperty("user.dir") + "/uploads/img/profile_img";
				File uploadPath = new File(uploadDir);
				if (!uploadPath.exists()) uploadPath.mkdirs();

				Path path = Paths.get(uploadDir, imageName);
				Files.copy(img.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return updatedUser;
	}

	@Override
	public UserDtls saveAdmin(UserDtls user) {
		user.setRole("ROLE_ADMIN");
		user.setIsEnable(true);
		user.setAccountNonLocked(true);
		user.setFailedAttempt(0);

		String encodePassword = passwordEncoder.encode(user.getPassword());
		user.setPassword(encodePassword);
		UserDtls saveUser = userRepository.save(user);
		return saveUser;
	}
	
	@Override
	public Boolean existsEmail(String email) {
		return userRepository.existsByEmail(email);
	}

}
