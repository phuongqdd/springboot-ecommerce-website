package com.dophuong.service;

import java.util.List;

import com.dophuong.model.OrderRequest;
import com.dophuong.model.ProductOrder;

public interface OrderService {
	public void saveOrder(Integer userid, OrderRequest orderRequest) throws Exception;

	public List<ProductOrder> getOrdersByUser(Integer userId);

	public ProductOrder updateOrderStatus(Integer id, String status);
	
	public List<ProductOrder> getAllOrders();
}
