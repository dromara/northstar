package tech.xuanwu.northstar.trader.controller;

import org.apache.commons.codec.binary.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.hash.Hashing;

import io.swagger.annotations.Api;
import tech.xuanwu.northstar.common.ResultBean;
import tech.xuanwu.northstar.common.ReturnCode;
import tech.xuanwu.northstar.utils.JwtUtil;

@Api(tags = "权限接口")
@RequestMapping("/")
@RestController
public class AuthenticationController {
	
	@Value("${auth.user}")
	private String user;
	@Value("${auth.password}")
	private String password;

	@PostMapping("/auth")
	public ResultBean<String> auth(@RequestParam String t, @RequestBody String encodeStr){
		Assert.hasText(encodeStr, "无效参数");
		Assert.hasText(t, "无效参数");
		String codecStr = Hashing.md5().hashBytes(String.format("%s&%s&%s", user, password, t).getBytes()).toString();
		if(StringUtils.equals(codecStr, encodeStr)) {
			return new ResultBean<String>(JwtUtil.sign(user, password));
		}
		return new ResultBean<>(ReturnCode.ERROR, "认证失败");
	}
	
}
