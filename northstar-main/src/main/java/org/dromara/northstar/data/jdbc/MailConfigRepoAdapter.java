package org.dromara.northstar.data.jdbc;

import java.util.Objects;

import org.dromara.northstar.common.model.MailConfigDescription;
import org.dromara.northstar.data.IMailConfigRepository;
import org.dromara.northstar.data.jdbc.entity.MailConfigDescriptionDO;

import com.alibaba.fastjson2.JSON;

public class MailConfigRepoAdapter implements IMailConfigRepository{

	private MailConfigDescriptionRepository delegate;
	
	private static final String THE_ONE = "THE_ONE_ID";
	
	public MailConfigRepoAdapter(MailConfigDescriptionRepository delegate) {
		this.delegate = delegate;
	}
	
	@Override
	public void save(MailConfigDescription configDescription) {
		delegate.save(new MailConfigDescriptionDO(THE_ONE, JSON.toJSONString(configDescription)));
	}

	@Override
	public MailConfigDescription get() {
		MailConfigDescriptionDO mcdDo = delegate.findById(THE_ONE).orElse(null);
		if(Objects.isNull(mcdDo)) {
			return null;
		}
		return JSON.parseObject(mcdDo.getDataStr(), MailConfigDescription.class);
	}

}
