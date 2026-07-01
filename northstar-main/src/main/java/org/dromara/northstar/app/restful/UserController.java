package org.dromara.northstar.app.restful;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.app.security.LoginIPLockService;
import org.dromara.northstar.app.security.LoginIPLockService.LockStatus;
import org.dromara.northstar.common.exception.IPBannedException;
import org.dromara.northstar.common.exception.IPFrozenException;
import org.dromara.northstar.common.exception.IllegalLoginException;
import org.dromara.northstar.common.exception.TooManyLoginException;
import org.dromara.northstar.common.exception.UserLockedException;
import org.dromara.northstar.common.model.NsUser;
import org.dromara.northstar.common.model.PayedServiceVO;
import org.dromara.northstar.config.UserInfo;
import org.dromara.northstar.support.utils.EventEmitterHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RequestMapping("northstar/user")
@RestController
public class UserController {
	
	@Autowired
	private HttpSession session;
	
	@Autowired
	private UserInfo user;
	
	@Autowired
	private Environment env;
	
	@Autowired
	private EventEmitterHolder emitterHolder;
	
	@Autowired
	private LoginIPLockService ipLockService;
	
	@Value("${quantit.datasource.secret:}")
	private String dsSecret;
	
	private static final Map<String, String> serviceNameMap = new HashMap<>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		{
			put("DS_CTP","数据服务_国内期货");
			put("DS_CRYPTO","数据服务_加密货币");
			put("DS_CNSTK","数据服务_A股");
			put("DS_USSTK","数据服务_美股");
			put("DS_HKSTK","数据服务_港股");
			put("DS_FOREX","数据服务_外汇");
			
		}
	};
	
	
	@PostMapping("login")
	public ResponseEntity<String> login(@RequestBody NsUser loginUser, HttpServletRequest req){
		if(emitterHolder.getEmitters().size() > 100) {
			throw new TooManyLoginException();
		}
		String ip = req.getRemoteAddr();
		LockStatus ipStatus = ipLockService.checkStatus(ip);
		if(ipStatus == LockStatus.BANNED) {
			throw new IPBannedException(ipLockService.getRemainingSeconds(ip));
		}
		if(ipStatus == LockStatus.FROZEN) {
			throw new IPFrozenException(ipLockService.getRemainingSeconds(ip));
		}
		session.invalidate();
		String usrname = loginUser.getUsername();
		String pwd = loginUser.getPassword();
		if(user.isLocked() && env.matchesProfiles("prod")) {
			throw new UserLockedException();
		}
		if(StringUtils.equals(user.getUserId(), usrname) && StringUtils.equals(user.getPassword(), pwd)) {
			String csrfToken = UUID.randomUUID().toString();
			session.setAttribute(Constants.SESS_CSRF, csrfToken);
			user.resetAttempt();
			ipLockService.clear(ip);
			return ResponseEntity.ok(csrfToken);
		}
		user.errorAttempt();
		LockStatus failureStatus = ipLockService.recordFailure(ip);
		switch(failureStatus) {
			case FROZEN -> throw new IPFrozenException(ipLockService.getRemainingSeconds(ip));
			case BANNED -> throw new IPBannedException(ipLockService.getRemainingSeconds(ip));
			default -> throw new IllegalLoginException();
		}
	}
	
	@GetMapping("logout")
	public void logout(){
		session.invalidate();
	}
	
	@GetMapping("payedServices")
	public List<PayedServiceVO> payedServices() {
		String token = System.getenv("NS_DS_SECRET");
    	if(StringUtils.isNotBlank(dsSecret)) {
    		token = dsSecret;
    	}
    	if(StringUtils.isEmpty(token)) {
    		return Collections.emptyList();
    	}
    	List<PayedServiceVO> results = new ArrayList<>();
    	try {
    		Map<String, Claim> claims = JWT.decode(token).getClaims();
    		for(Map.Entry<String, Claim> e : claims.entrySet()) {
    			String key = e.getKey();
    			if(serviceNameMap.containsKey(key)) {
    				results.add(PayedServiceVO.builder().name(serviceNameMap.get(key)).expiredDate(LocalDate.parse(e.getValue().asString())).build());
    			}
    		}
    	} catch (Exception e) {
    		throw new IllegalStateException("JWT解析异常：" + token, e);
		}
		return results;
	}
	
	
	@GetMapping("validate")
	public boolean testSession(){
		return !session.isNew() && Objects.nonNull(session.getAttribute(Constants.SESS_CSRF));
	}
	
}
