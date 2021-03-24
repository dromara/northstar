package tech.xuanwu.northstar.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.xuanwu.northstar.common.exception.AuthenticationException;
import tech.xuanwu.northstar.common.model.NsUser;
import tech.xuanwu.northstar.utils.JwtUtil;

/**
 * 身份认证
 * @author KevinHuangwl
 *
 */
@RestController
@RequestMapping("/auth")
public class AuthenticationController {
	
	@Value("${auth.userId}")
	protected String userId;
	@Value("${auth.password}")
	protected String password;

	@PostMapping(value="/token", produces = "application/json")
	public String doAuth(@RequestBody NsUser user) {
		Assert.hasText(user.getUserName(), "账户不能为空");
		Assert.hasText(user.getPassword(), "密码不能为空");
		if(StringUtils.equals(user.getUserName(), userId) && StringUtils.equals(user.getPassword(), password)) {
			return JwtUtil.sign(user.getUserName(), user.getPassword());
		}
		throw new AuthenticationException("账户或密码不正确");
	}
}
