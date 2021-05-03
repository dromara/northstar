package tech.xuanwu.northstar.service;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;

import tech.xuanwu.northstar.common.constant.Constants;
import tech.xuanwu.northstar.common.model.NsUser;

public class BaseService {

	@Autowired
	protected HttpSession session;
	
	protected String getUserName() {
//		NsUser user = (NsUser) session.getAttribute(Constants.KEY_USER);
//		return user.getUserName();
		return "admin";
	}
}
