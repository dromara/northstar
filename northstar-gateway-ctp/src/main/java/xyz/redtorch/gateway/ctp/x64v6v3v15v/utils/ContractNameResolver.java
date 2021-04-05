package xyz.redtorch.gateway.ctp.x64v6v3v15v.utils;

import java.io.IOException;
import java.io.InputStream;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public abstract class ContractNameResolver {

	static JSONObject contractNameDict;
	
	static {
		//加载合约中文解释
		InputStream is = ContractNameResolver.class.getClassLoader().getResourceAsStream("CTP期货合约中文对照表.json");
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
	
	public static void main(String[] args) {
		System.out.println(getCNSymbolName("rb2102"));
	}
}
