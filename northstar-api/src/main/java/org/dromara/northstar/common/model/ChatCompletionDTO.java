package org.dromara.northstar.common.model;

import java.util.List;

import lombok.Data;

@Data
public class ChatCompletionDTO {

	private String helper;
	
	private String platform;
	
	private List<ContentDTO> contents;
	
	@Data
	public static class ContentDTO {
		
		private String role;
		
		private String content;
		
	}
}
