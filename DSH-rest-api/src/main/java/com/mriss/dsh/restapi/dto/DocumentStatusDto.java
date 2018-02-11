package com.mriss.dsh.restapi.dto;

public class DocumentStatusDto {
	
	private String status;
	private String message;
	
	public DocumentStatusDto() {
		super();
	}

	public DocumentStatusDto(String status, String message) {
		this();
		this.status = status;
		this.message = message;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
