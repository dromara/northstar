package org.dromara.northstar.web.restful;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.exception.AuthenticationException;
import org.dromara.northstar.common.model.NsUser;
import org.dromara.northstar.common.model.ResultBean;
import org.dromara.northstar.config.UserInfo;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import cn.hutool.crypto.digest.MD5;
import lombok.extern.slf4j.Slf4j;

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
	
	@Value("${northstar.socketio}")
	protected int socketioPort;
	
	private LocalDate errDate = LocalDate.now();
	private AtomicInteger errCnt = new AtomicInteger();
	private static final int MAX_ATTEMPT = 5;
	
	@PostMapping(value="/login", produces = "application/json")
	public ResultBean<String> doAuth(long timestamp, @RequestBody NsUser user) {
		if(errCnt.get() >= MAX_ATTEMPT) {
			throw new AuthenticationException("超过重试限制，登陆受限");
		}
		Assert.hasText(user.getUserName(), "用户名不能为空");
		Assert.hasText(user.getPassword(), "密码不能为空");
		Assert.isTrue(Math.abs(timestamp - System.currentTimeMillis()) < 30000, "使用了非法登陆时间戳，请同步校准电脑时间");
		String encodedPassword = MD5.create().digestHex((userInfo.getPassword() + timestamp));
		if(StringUtils.equals(user.getUserName(), userInfo.getUserId()) && StringUtils.equals(user.getPassword(), encodedPassword)) {
			session.setAttribute(Constants.KEY_USER, user);
			errCnt.set(0);
			return new ResultBean<>("OK");
		}
		LocalDate curDate = LocalDate.now();
		if(!errDate.equals(curDate)) {
			errDate = curDate;
			errCnt.set(0);
		}
		errCnt.incrementAndGet();
		throw new AuthenticationException("用户名或密码不正确");
	}
	
	@RequestMapping(path = "/login", method = RequestMethod.HEAD)
	public ResultBean<String> healthCheck() {
		return new ResultBean<>("OK");
	}
	
	@GetMapping("/socketio")
	public ResultBean<Integer> socketioPort(){
		return new ResultBean<>(socketioPort);
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
