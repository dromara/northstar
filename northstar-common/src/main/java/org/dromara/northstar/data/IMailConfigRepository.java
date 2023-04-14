package org.dromara.northstar.data;

import org.dromara.northstar.common.model.MailConfigDescription;

public interface IMailConfigRepository {
	
	/**
	 * 保存或修改
	 * @param configDescription
	 */
	void save(MailConfigDescription configDescription);
	/**
	 * 查询
	 * @return
	 */
	MailConfigDescription get();
}
