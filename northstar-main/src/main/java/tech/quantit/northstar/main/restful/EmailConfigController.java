package tech.quantit.northstar.main.restful;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.quantit.northstar.common.model.MailConfigDescription;
import tech.quantit.northstar.common.model.ResultBean;
import tech.quantit.northstar.main.service.EmailConfigService;

/**
 * @author KevinHuangwl
 *
 */
@RequestMapping("/northstar/email")
@RestController
public class EmailConfigController {
	
	@Autowired
	private EmailConfigService service;

	@PostMapping
	public ResultBean<Boolean> save(@NotNull @RequestBody MailConfigDescription configDescription, boolean enabled){
		service.saveConfig(configDescription, enabled);
		return new ResultBean<>(Boolean.TRUE);
	}
	
	@GetMapping
	public ResultBean<MailConfigDescription> get(){
		return new ResultBean<>(service.getConfig());
	}
}
