package org.dromara.northstar.common.model.core;

import lombok.Builder;
import xyz.redtorch.pb.CoreEnum.CommonStatusEnum;

@Builder
public record Notice(
		CommonStatusEnum status,
		String content
	) {

}
