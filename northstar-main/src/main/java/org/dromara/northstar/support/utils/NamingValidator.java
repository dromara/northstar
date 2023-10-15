package org.dromara.northstar.support.utils;

import java.util.regex.Pattern;

public class NamingValidator {

	private static final Pattern ptn = Pattern.compile("^[\\u4e00-\\u9fa5a-zA-Z0-9_]+$");
	
	public boolean isValid(String name) {
		return ptn.matcher(name).matches();
	}
}
