package org.dromara.northstar.common.model.core;

import lombok.Builder;
import xyz.redtorch.pb.CoreEnum.CommonStatusEnum;
import xyz.redtorch.pb.CoreField.NoticeField;

@Builder
public record Notice(
		CommonStatusEnum status,
		String content
	) {

	public NoticeField toNoticeField() {
		return NoticeField.newBuilder()
				.setContent(content)
				.setStatus(status)
				.build();
	}
}
