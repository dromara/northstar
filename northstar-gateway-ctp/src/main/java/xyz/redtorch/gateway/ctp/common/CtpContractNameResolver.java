package xyz.redtorch.gateway.ctp.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class CtpContractNameResolver {

	private CtpContractNameResolver() {}
	
	private static JSONObject contractNameDict;
	
	private static final Pattern futureNamePtn = Pattern.compile("([A-z]+)(\\d+)");
	private static final Pattern unifiedSymbolPtn = Pattern.compile("(\\w+)@\\w+@\\w+");
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
		String contract = symbol.replaceAll("\\d+", "");
		String cname = contractNameDict.getString(contract);
		if(cname == null) {
			return null;
		}
		return symbol.replace(contract, cname);
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
