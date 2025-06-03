package com.dophuong.util;

import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.dophuong.model.ProductOrder;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class CommonUtil {

	@Autowired
	private JavaMailSender mailSender;

	public Boolean sendMail(String url, String emailNguoiNhan) throws UnsupportedEncodingException, MessagingException {

		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);

		// Email người gửi và tên hiển thị
		helper.setFrom("doquangphuong.nd2003@gmail.com", "DoPhuong Store");

		// Email người nhận
		helper.setTo(emailNguoiNhan);

		// Nội dung email
		String noiDung = "<p>Xin chào,</p>" + "<p>Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản của mình.</p>"
				+ "<p>Vui lòng nhấn vào liên kết bên dưới để thay đổi mật khẩu:</p>" + "<p><a href=\"" + url
				+ "\"><b>Đổi mật khẩu</b></a></p>" + "<br>"
				+ "<p>Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này.</p>"
				+ "<p>Trân trọng,<br>DoPhuong Store</p>";

		// Tiêu đề email
		helper.setSubject("Yêu cầu đặt lại mật khẩu");

		// Gửi nội dung dưới dạng HTML
		helper.setText(noiDung, true);

		// Gửi email
		mailSender.send(message);
		return true;
	}

	public static String generateUrl(HttpServletRequest request) {

		// http://localhost:8080/forgot-password
		String siteUrl = request.getRequestURL().toString();

		return siteUrl.replace(request.getServletPath(), "");

	}

	public Boolean sendMailForProductOrder(ProductOrder order, String status) throws Exception {

		String msg = "<div style='font-family: Arial, sans-serif; line-height: 1.6; padding: 10px;'>"
				+ "<h2 style='color: #0d6efd;'>Xin chào [[name]],</h2>"
				+ "<p>Cảm ơn bạn đã đặt hàng tại <strong>DoPhuong Store</strong>.</p>"
				+ "<p>Trạng thái đơn hàng hiện tại của bạn là: "
				+ "<strong style='color: #198754;'>[[orderStatus]]</strong></p>"

				+ "<h4 style='margin-top: 30px;'>Thông tin đơn hàng:</h4>"
				+ "<table style='border-collapse: collapse; width: 100%;'>"
				+ "  <tr><td><strong>Tên sản phẩm:</strong></td><td>[[productName]]</td></tr>"
				+ "  <tr><td><strong>Danh mục:</strong></td><td>[[category]]</td></tr>"
				+ "  <tr><td><strong>Số lượng:</strong></td><td>[[quantity]]</td></tr>"
				+ "  <tr><td><strong>Giá:</strong></td><td>[[price]]</td></tr>"
				+ "  <tr><td><strong>Phương thức thanh toán:</strong></td><td>[[paymentType]]</td></tr>" 
				+ "</table>"

				+ "<p style='margin-top: 30px;'>Bạn có thể truy cập website của chúng tôi để theo dõi đơn hàng.</p>"
				+ "<p style='color: #6c757d;'>Trân trọng,<br><strong>DoPhuong Store</strong></p>"
				+ "<hr style='margin-top: 30px;'>"
				+ "<p style='font-size: 12px; color: gray;'>Email này được gửi tự động. Vui lòng không trả lời thư này.</p>"
				+ "</div>";

		// Tạo và cấu hình mail
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

		helper.setFrom("doquangphuong.nd2003@gmail.com", "DoPhuong Store");
		helper.setTo(order.getOrderAddress().getEmail());

		// Format tiền tệ VNĐ
		DecimalFormat formatter = new DecimalFormat("#,###");
		String formattedPrice = formatter.format(order.getPrice()) + " VNĐ";

		// Thay thế các trường trong template
		msg = msg.replace("[[name]]", order.getOrderAddress().getFirstName());
		msg = msg.replace("[[orderStatus]]", status);
		msg = msg.replace("[[productName]]", order.getProduct().getTitle());
		msg = msg.replace("[[category]]", order.getProduct().getCategory());
		msg = msg.replace("[[quantity]]", order.getQuantity().toString());
		msg = msg.replace("[[price]]", formattedPrice);
		msg = msg.replace("[[paymentType]]", order.getPaymentType());

		// Thiết lập nội dung mail
		helper.setSubject("Cập nhật đơn hàng của bạn - DoPhuong Store");
		helper.setText(msg, true);

		mailSender.send(message);
		return true;
	}

}
