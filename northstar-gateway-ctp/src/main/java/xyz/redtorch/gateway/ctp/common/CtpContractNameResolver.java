package xyz.redtorch.gateway.ctp.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class CtpContractNameResolver {

	private CtpContractNameResolver() {}
	
	private static JSONObject contractNameDict;
	
	private static final Pattern futureNamePtn = Pattern.compile("([A-z]+)(\\d+)");
	private static final Pattern unifiedSymbolPtn = Pattern.compile("(\\w+)@\\w+@\\w+");
	private static final Pattern symbolPtn = Pattern.compile("([A-z]+)([0-9]*)-?([CP]?)-?([0-9]*)");
	private static final String DEFAULT_GROUP = "default";
	
	static {
		//加载合约中文解释
		InputStream is = CtpContractNameResolver.class.getClassLoader().getResourceAsStream("CTPSymbolNameMapping.json");
		try {
			byte[] buf = new byte[1024*3];
			StringBuilder sb = new StringBuilder();
			int len = 0;
			while((len = is.read(buf)) != -1) {
				sb.append(new String(buf, 0, len,"UTF-8"));
			}
			contractNameDict = JSON.parseObject(sb.toString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String getCNSymbolName(String symbol) {
		Matcher m = symbolPtn.matcher(symbol);
		if(m.find()) {
			String contract = m.group(1);
			String cname = contractNameDict.getString(contract);
			String monthYear = m.group(2);
			String callPut = m.group(3);
			String price = m.group(4);
			return String.format("%s%s%s%s", 
					Optional.ofNullable(cname).orElse(contract), 
					monthYear, 
					Optional.of(callPut).orElse("").replace("C", "买权").replace("P", "卖权"),
					Optional.of(price).orElse(""));
		}
		
		return null;
	}
	
	public static String symbolToSymbolGroup(String symbol) {
		if(futureNamePtn.matcher(symbol).matches()) {			
			return symbol.replaceAll(futureNamePtn.pattern(), "$1");
		}
		return DEFAULT_GROUP;
	}
	
	public static String unifiedSymbolToSymbolGroup(String unifiedSymbol) {
		String symbol = unifiedSymbol.replaceAll(unifiedSymbolPtn.pattern(), "$1");
		return symbolToSymbolGroup(symbol);
	}
	
}
