package tech.quantit.northstar.gateway.api.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.common.constant.GatewayType;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 合约工厂，负责根据网关回报的合约信息创建Contract与IndexContract
 * @author KevinHuangwl
 *
 */
public class ContractFactory {

	private List<ContractField> contractList;
	
	private GatewayType gatewayType;
	
	private static final Pattern symbolPtn = Pattern.compile("([A-z]+)\\d{3,4}(@\\w+@\\w+)");
	
	private static final ProductClassEnum indexProductClass = ProductClassEnum.FUTURES;
	
	public ContractFactory(GatewayType gatewayType, List<ContractField> contractList) {
		this.contractList = contractList;
		this.gatewayType = gatewayType;
	}
	
	public List<NormalContract> makeNormalContract(){
		return contractList.stream().map(cf -> new NormalContract(cf, gatewayType)).toList();
	}
	
	public List<IndexContract> makeIndexContract(){
		Map<String, Set<ContractField>> idxSrcMap = new HashMap<>();
		for(ContractField cf : contractList) {
			// 暂时只对期货合约生成指数
			if(cf.getProductClass() != indexProductClass) {
				continue;
			}
			String unifiedSymbol = cf.getUnifiedSymbol();
			Matcher m = symbolPtn.matcher(unifiedSymbol);
			if(m.find()) {
				String groundName = m.group(1);
				String others = m.group(2);
				String symbol = groundName + Constants.INDEX_SUFFIX + others;
				if(!idxSrcMap.containsKey(symbol)) {					
					idxSrcMap.putIfAbsent(symbol, new HashSet<>());
				}
				idxSrcMap.get(symbol).add(cf);
			}
		}
		return idxSrcMap.entrySet().stream().map(e -> new IndexContract(e.getKey(), e.getValue())).toList();
	}
}
