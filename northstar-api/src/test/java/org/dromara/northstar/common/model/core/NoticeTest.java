package org.dromara.northstar.common.model.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreEnum.CommonStatusEnum;

class NoticeTest {

	@Test
	void testToNoticeField() {
		Notice notice = Notice.builder()
				.content("content")
				.status(CommonStatusEnum.COMS_INFO)
				.build();
		xyz.redtorch.pb.CoreField.NoticeField noticeField = notice.toNoticeField();
		assertEquals(notice.content(), noticeField.getContent());
		assertEquals(notice.status(), noticeField.getStatus());
	}

}
