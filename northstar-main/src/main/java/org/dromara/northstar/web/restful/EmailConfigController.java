package org.dromara.northstar.web.restful;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.MailConfigDescription;
import org.dromara.northstar.common.model.ResultBean;
import org.dromara.northstar.data.IMailConfigRepository;
import org.dromara.northstar.notification.MailDeliveryManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import xyz.redtorch.pb.CoreEnum.CommonStatusEnum;
import xyz.redtorch.pb.CoreField.NoticeField;

/**
 * @author KevinHuangwl
 *
 */
@RequestMapping("/northstar/email")
@RestController
public class EmailConfigController implements InitializingBean{
	
	@Autowired
	private MailDeliveryManager mailMgr;
	
	@Autowired
	private IMailConfigRepository mailConfigRepo;
	
	@PostMapping
	public ResultBean<Boolean> save(@RequestBody MailConfigDescription configDescription){
		Assert.notNull(configDescription, "邮件配置信息不能为空");
		mailConfigRepo.save(configDescription);
		mailMgr.setEmailConfig(configDescription);
		return new ResultBean<>(Boolean.TRUE);
	}
	
	@GetMapping
	public ResultBean<MailConfigDescription> get(){
		return new ResultBean<>(mailConfigRepo.get());
	}
	
	@GetMapping("testable")
	public ResultBean<Boolean> testable(){
		MailConfigDescription cfg = mailConfigRepo.get();
		return new ResultBean<>(StringUtils.isNotEmpty(cfg.getEmailPassword()) && StringUtils.isNotEmpty(cfg.getEmailUsername()) && !cfg.getSubscriberList().isEmpty());
	}
	
	@GetMapping("test")
	public void testMail() {
		mailMgr.onEvent(new NorthstarEvent(NorthstarEventType.NOTICE, NoticeField.newBuilder()
				.setStatus(CommonStatusEnum.COMS_INFO)
				.setContent("邮件测试")
				.setTimestamp(System.currentTimeMillis())
				.build()));
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		mailMgr.setEmailConfig(mailConfigRepo.get());
	}
}
