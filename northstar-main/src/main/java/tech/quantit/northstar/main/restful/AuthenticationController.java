package tech.quantit.northstar.main.restful;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.common.exception.AuthenticationException;
import tech.quantit.northstar.common.model.NsUser;
import tech.quantit.northstar.common.model.ResultBean;
import tech.quantit.northstar.main.config.UserInfo;

/**
 * 身份认证
 * @author KevinHuangwl
 *
 */
@Slf4j
@RestController
@RequestMapping("/northstar/auth")
public class AuthenticationController implements InitializingBean{
	
	@Autowired
	protected UserInfo userInfo;
	
	@Autowired
	protected HttpSession session;
	
	@PostMapping(value="/login", produces = "application/json")
	public ResultBean<String> doAuth(@RequestBody NsUser user) {
		Assert.hasText(user.getUserName(), "账户不能为空");
		Assert.hasText(user.getPassword(), "密码不能为空");
		if(StringUtils.equals(user.getUserName(), userInfo.getUserId()) && StringUtils.equals(user.getPassword(), userInfo.getPassword())) {
			session.setAttribute(Constants.KEY_USER, user);
			return new ResultBean<>("OK");
		}
		throw new AuthenticationException("账户或密码不正确");
	}

	@GetMapping("/logout")
	public void logout() {
		session.invalidate();
	}
	
	@GetMapping("/test")
	public ResultBean<Boolean> testAuth() {
		return new ResultBean<>(Boolean.TRUE);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		log.debug("监控台登陆信息：{} / {}", userInfo.getUserId(), userInfo.getPassword());
	}
}
