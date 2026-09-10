package com.kh.midpoint.common.response;

import lombok.Getter;

@Getter
public class ApiResponse<T> {

	private final int code;
	private final String message;
	private final T data;

	private ApiResponse(int code, String message, T data) {
		this.code = code;
		this.message = message;
		this.data = data;
	}

	public static <T> ApiResponse<T> ok(String message, T data) {
		return new ApiResponse<>(200, message, data);
	}

	public static <T> ApiResponse<T> created(String message, T data) {
		return new ApiResponse<>(201, message, data);
	}

	public static <T> ApiResponse<T> error(int code, String message) {
		return new ApiResponse<>(code, message, null);
	}

}
