package com.dophuong.util;

public enum OrderStatus {
	IN_PROGRESS(1, "Đang xử lý"),
    ORDER_RECEIVED(2, "Đã tiếp nhận"),
    PRODUCT_PACKED(3, "Đã đóng gói"),
    OUT_FOR_DELIVERY(4, "Đang giao hàng"),
    DELIVERED(5, "Đã giao thành công"),
    CANCELLED(6, "Đã huỷ"),
    SUCCESS(7, "Thành công");

	private final Integer id;
	private final String name;

	OrderStatus(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public static OrderStatus fromId(Integer id) {
		for (OrderStatus status : OrderStatus.values()) {
			if (status.getId().equals(id)) {
				return status;
			}
		}
		return null;
	}
}

