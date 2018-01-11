package com.mriss.dsh.restapi.dto;

public class TokenDto {
	
	private String token;
	private String message;
	
	public TokenDto() {
		super();
	}

	public TokenDto(String token, String message) {
		this();
		this.token = token;
		this.message = message;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
