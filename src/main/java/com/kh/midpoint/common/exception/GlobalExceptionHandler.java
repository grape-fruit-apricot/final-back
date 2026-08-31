package com.kh.midpoint.common.exception;

import com.kh.midpoint.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ApiResponse<Object>> handleNotFoundException(NotFoundException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage()));
	}

	@ExceptionHandler(DuplicateException.class)
	public ResponseEntity<ApiResponse<Object>> handleDuplicateException(DuplicateException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(ApiResponse.error(HttpStatus.CONFLICT.value(), e.getMessage()));
	}

	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<ApiResponse<Object>> handleForbiddenException(ForbiddenException e) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
			.body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), e.getMessage()));
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ApiResponse<Object>> handleUnauthorizedException(UnauthorizedException e) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), e.getMessage()));
	}

	@ExceptionHandler(InvalidStateException.class)
	public ResponseEntity<ApiResponse<Object>> handleInvalidStateException(InvalidStateException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
	}

	@ExceptionHandler(ExternalApiException.class)
	public ResponseEntity<ApiResponse<Object>> handleExternalApiException(ExternalApiException e) {
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
			.body(ApiResponse.error(HttpStatus.BAD_GATEWAY.value(), e.getMessage()));
	}

}
