package xyz.redtorch.gateway.ctp.common;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.gateway.api.domain.NormalContract;
import tech.quantit.northstar.gateway.api.domain.SubscriptionManager;

/**
 * CTP网关合约订阅管理器
 * 负责设置合约订阅的黑白名单
 * 配置方式：
 * 在配置文件中定义subscription.ctp.classType.whitelist，设置合约种类的白名单
 * 在配置文件中定义subscription.ctp.classType.blacklist，设置合约种类的黑名单
 * 在配置文件中定义subscription.ctp.unifiedSymbol.whitelist，设置合约种类的白名单
 * 在配置文件中定义subscription.ctp.unifiedSymbol.blacklist，设置合约种类的黑名单
 * 配置值采用正则表达式匹配
 * 如果用户同时提供白名单与黑名单，则只会订阅在白名单中且不在黑名单中的合约
 * @author KevinHuangwl
 *
 */
public class CtpSubscriptionManager implements SubscriptionManager {
	
	private Pattern ptnClzWht;
	private Pattern ptnClzBlk;
	private Pattern ptnSymWht;
	private Pattern ptnSymBlk;
	
	public CtpSubscriptionManager(String classTypeWhitelist, String classTypeBlacklist, String symbolWhitelist, String symbolBlacklist) {
		if(StringUtils.isNotBlank(classTypeWhitelist))
			ptnClzWht = Pattern.compile(classTypeWhitelist, Pattern.CASE_INSENSITIVE);
		if(StringUtils.isNotBlank(classTypeBlacklist))
			ptnClzBlk = Pattern.compile(classTypeBlacklist, Pattern.CASE_INSENSITIVE);
		if(StringUtils.isNotBlank(symbolWhitelist))
			ptnSymWht = Pattern.compile(symbolWhitelist, Pattern.CASE_INSENSITIVE);
		if(StringUtils.isNotBlank(symbolBlacklist))
			ptnSymBlk = Pattern.compile(symbolBlacklist, Pattern.CASE_INSENSITIVE);
	}

	@Override
	public boolean subscribable(NormalContract contract) {
		boolean inClzBlklist = ptnClzBlk != null && ptnClzBlk.matcher(contract.productClass().toString()).find();
		boolean inSymBlklist = ptnSymBlk != null && ptnSymBlk.matcher(contract.unifiedSymbol()).find();
		boolean notInSymWhtlist = ptnSymWht != null && !ptnSymWht.matcher(contract.unifiedSymbol()).find();
		boolean notInClzWhtlist = ptnClzWht != null && !ptnClzWht.matcher(contract.productClass().toString()).find();
		return  !inClzBlklist && !inSymBlklist && !notInSymWhtlist && !notInClzWhtlist;
	}

	@Override
	public GatewayType usedFor() {
		return GatewayType.CTP;
	}

}
