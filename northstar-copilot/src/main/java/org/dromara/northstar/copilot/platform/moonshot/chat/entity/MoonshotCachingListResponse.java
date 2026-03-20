package org.dromara.northstar.copilot.platform.moonshot.chat.entity;

import java.util.List;

import lombok.Data;

@Data
public class MoonshotCachingListResponse {

	private String object;
	
	private List<MoonshotCachingResponse> data;
}
