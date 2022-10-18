package tech.quantit.northstar.main.restful;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.model.MailConfigDescription;
import tech.quantit.northstar.common.model.ResultBean;
import tech.quantit.northstar.main.service.EmailConfigService;
import xyz.redtorch.pb.CoreEnum.CommonStatusEnum;
import xyz.redtorch.pb.CoreField.NoticeField;

/**
 * @author KevinHuangwl
 *
 */
@RequestMapping("/northstar/email")
@RestController
public class EmailConfigController {
	
	@Autowired
	private EmailConfigService service;

	@Autowired
	private FastEventEngine feEngine;
	
	@PostMapping
	public ResultBean<Boolean> save(@NotNull @RequestBody MailConfigDescription configDescription){
		service.saveConfig(configDescription);
		return new ResultBean<>(Boolean.TRUE);
	}
	
	@GetMapping
	public ResultBean<MailConfigDescription> get(){
		return new ResultBean<>(service.getConfig());
	}
	
	@GetMapping("test")
	public void testMail() {
		NoticeField notice = NoticeField.newBuilder()
				.setStatus(CommonStatusEnum.COMS_INFO)
				.setTimestamp(System.currentTimeMillis())
				.setContent("just testing")
				.build();
		
		feEngine.emitEvent(NorthstarEventType.NOTICE, notice);
	}
}
