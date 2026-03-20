package org.dromara.northstar.app.restful;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.dromara.northstar.app.service.MetaDataService;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.ComponentField;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.GatewayTypeDescription;
import org.dromara.northstar.common.model.StrategyMetaInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("northstar/metadata")
@RestController
public class MetaDataController {
	
	@Autowired
	MetaDataService service;
	
	@Autowired
	BuildProperties buildInfo;
	
	@Autowired
	Environment env;
	
	private static final int MAX_PAGE_SIZE = 100;
	
	/**
	 * 可用合约
	 * @return
	 */
	@GetMapping("contracts/{channel}")
	public List<ContractSimpleInfo> availableContracts(@PathVariable ChannelType channel, String query){
		List<ContractSimpleInfo> results = service.availableContracts(channel, query);
		if(results.size() > MAX_PAGE_SIZE) {
			return results.subList(0, MAX_PAGE_SIZE);
		}
		return results;
	}
	
	/**
	 * 已订阅合约
	 * @return
	 */
	@GetMapping("contracts/{channel}/sub")
	public List<ContractSimpleInfo> subscribedContracts(@PathVariable ChannelType channel, String query){
		List<ContractSimpleInfo> results = service.subscribedContracts(channel, query);
		if(results.size() > MAX_PAGE_SIZE) {
			return results.subList(0, MAX_PAGE_SIZE);
		}
		return results;
	}
	
	/**
	 * 已订阅合约
	 * @param channels
	 * @param query
	 * @return
	 */
	@GetMapping("contracts/sub")
	public List<ContractSimpleInfo> subContractsOfAccounts(String[] accountIds, String query){
		List<ContractSimpleInfo> results = service.subContractsOfAccounts(accountIds, query);
		if(results.size() > MAX_PAGE_SIZE) {
			return results.subList(0, MAX_PAGE_SIZE);
		}
		return results;
	}
	
	/**
	 * 网关配置元信息
	 * @return
	 */
	@GetMapping("gateway/settings")
	public Map<ChannelType, Collection<ComponentField>> gatewaySettingsMeta(){
		return service.gatewaySettingsMeta();
	}
	
	/**
	 * 网关渠道类型
	 * @return
	 */
	@GetMapping("channels")
	public List<GatewayTypeDescription> channelsMeta(){
		return service.channelsMeta();
	}
	
	/**
	 * 策略配置元信息
	 * @return
	 */
	@GetMapping("strategy/settings")
	public List<StrategyMetaInfo> moduleSettingsMeta(){
		return service.strategySettingsMeta();
	}
	
	/**
	 * 可订阅事件
	 * @return
	 */
	@GetMapping("notifiable/events")
	public List<NorthstarEventType> eventMeta(){
		return service.notifiableEvents();
	}
	
	/**
	 * 消息配置元信息
	 * @return
	 */
	@GetMapping("msgSender/settings")
	public Map<String, Collection<ComponentField>> notificationSettingsMeta(){
		return service.msgSenderSettingsMeta();
	}
	
	/**
	 * 版本信息
	 * @return
	 */
	@GetMapping("version")
	public String version(){
		return buildInfo.getVersion();
	}
	
	/**
	 * 运行环境信息
	 * @param env
	 * @return
	 */
	@GetMapping("profiles")
	public String[] profile() {
		return env.getActiveProfiles();
	}
}
