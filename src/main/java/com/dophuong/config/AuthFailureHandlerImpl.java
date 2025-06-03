package com.dophuong.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.dophuong.model.UserDtls;
import com.dophuong.repository.UserRepository;
import com.dophuong.service.UserService;
import com.dophuong.util.AppConstant;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthFailureHandlerImpl extends SimpleUrlAuthenticationFailureHandler {

    @Autowired
	private UserRepository userRepository;

	@Autowired
	private UserService userService;

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
	                                     AuthenticationException exception) throws IOException, ServletException {

	    String email = request.getParameter("username");

	    UserDtls userDtls = userRepository.findByEmail(email);

	    if (userDtls != null) {

	        if (userDtls.getIsEnable()) {

	            if (userDtls.getAccountNonLocked()) {

	                if (userDtls.getFailedAttempt() < AppConstant.ATTEMPT_TIME) {
	                    userService.increaseFailedAttempt(userDtls);
	                } else {
	                    userService.userAccountLock(userDtls);
	                    exception = new LockedException("Tài khoản của bạn đã bị khóa do đăng nhập sai 3 lần.");
	                }

	            } else {

	                if (userService.unlockAccountTimeExpired(userDtls)) {
	                    exception = new LockedException("Tài khoản của bạn đã được mở khóa. Vui lòng đăng nhập lại.");
	                } else {
	                    exception = new LockedException("Tài khoản của bạn đang bị khóa. Vui lòng thử lại sau ít phút.");
	                }
	            }

	        } else {
	            exception = new LockedException("Tài khoản của bạn chưa được kích hoạt.");
	        }

	    } else {
	        exception = new LockedException("Email hoặc mật khẩu không đúng.");
	    }

	    super.setDefaultFailureUrl("/signin?error");
	    super.onAuthenticationFailure(request, response, exception);
	}


}
