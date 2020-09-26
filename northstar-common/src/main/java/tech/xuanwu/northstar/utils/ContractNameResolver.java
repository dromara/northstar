package tech.xuanwu.northstar.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public abstract class ContractNameResolver {

	static JSONObject contractNameDict;
	
	static {
		//加载合约中文解释
		Resource dictFile = new ClassPathResource("/contract.json");
		ReadableByteChannel channel;
		try {
			channel = dictFile.readableChannel();
			ByteBuffer buf = ByteBuffer.allocate(128);
			StringBuilder sb = new StringBuilder();
			int len = 0;
			while((len = channel.read(buf)) != -1) {
				buf.flip();
				sb.append(new String(buf.array(), 0, len,"UTF-8"));
				buf.clear();
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
}
