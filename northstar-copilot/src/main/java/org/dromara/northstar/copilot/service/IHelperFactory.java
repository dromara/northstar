package org.dromara.northstar.copilot.service;

import java.util.List;

import org.dromara.northstar.common.model.ChatCompletionDTO.ContentDTO;
import org.dromara.northstar.copilot.platform.openai.chat.entity.ChatCompletion;

public interface IHelperFactory {

	ChatCompletion actAsBeginnerHelper(List<ContentDTO> contents);
	
	ChatCompletion actAsContractLookupHelper(List<ContentDTO> contents);
	
	ChatCompletion actAsStrategyHelper(List<ContentDTO> contents);
}
