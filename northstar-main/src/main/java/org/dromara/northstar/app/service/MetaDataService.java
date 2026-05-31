package org.dromara.northstar.app.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.dromara.northstar.account.GatewayManager;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.exception.NoSuchGatewayException;
import org.dromara.northstar.common.model.ComponentField;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.GatewayTypeDescription;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.StrategyMetaInfo;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.contract.IndexContract;
import org.dromara.northstar.gateway.contract.OptionChainContract;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.meta.GatewayMetaProvider;
import org.dromara.northstar.meta.MsgSenderMetaProvider;
import org.dromara.northstar.strategy.DynamicParamsAware;
import org.dromara.northstar.strategy.StrategicComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MetaDataService {
	
	@Autowired
	private MarketCenter marketCenter;
	
	@Autowired
	private GatewayMetaProvider gatewayMetaProvider;
	
	@Autowired
	private MsgSenderMetaProvider msgSenderMetaProvider;
	
	@Autowired
	private ApplicationContext ctx;
	
	@Autowired
	private GatewayManager gatewayMgr;
	
	public List<ContractSimpleInfo> availableContracts(ChannelType channel, String query) {
		return marketCenter.getContracts(channel)
				.stream()
				.map(ic -> ic.contract())
				.map(ContractSimpleInfo::from)
				.filter(c -> doFilter(c, query))
				.toList();
	}
	
	public List<ContractSimpleInfo> subscribedContracts(ChannelType channel, String query) {
		List<ContractSimpleInfo> resultList = new LinkedList<>();
		Gateway gateway = gatewayMgr.get(Identifier.of(channel.toString()));
		if(gateway == null) {
			throw new NoSuchGatewayException(channel.toString());
		}
		List<ContractSimpleInfo> subscribedList = gateway.getDescription().getSubscribedContracts();
		resultList.addAll(subscribedList);

		for(ContractSimpleInfo c : subscribedList) {
			IContract ic = marketCenter.getContract(Identifier.of(c.getId()));
			if(ic instanceof IndexContract idx) {
				resultList.addAll(idx.memberContracts()
						.stream()
						.map(IContract::contract)
						.map(ContractSimpleInfo::from)
						.toList());
			}
			if(ic instanceof OptionChainContract occ) {
				resultList.addAll(occ.memberContracts()
						.stream()
						.map(IContract::contract)
						.map(ContractSimpleInfo::from)
						.toList());
			}
		}
		
		return resultList.stream()
				.filter(c -> doFilter(c, query))
				.toList();
	}
	
	private boolean doFilter(ContractSimpleInfo c, String query) {
		Pattern ptn = Pattern.compile(query.replace("*", ".*"), Pattern.CASE_INSENSITIVE);
		return !StringUtils.hasText(query) || ptn.matcher(c.getName()).find() || ptn.matcher(c.getUnifiedSymbol()).find();
	}
	
	public List<ContractSimpleInfo> subContractsOfAccounts(String[] accountIds, String query) {
		return Set.of(accountIds).stream()
				.map(id -> gatewayMgr.get(Identifier.of(id)))
				.map(gw -> gw.getDescription().getBindedMktGatewayId())
				.filter(StringUtils::hasText)
				.collect(Collectors.toSet())
				.stream()
				.map(id -> gatewayMgr.get(Identifier.of(id)))
				.map(gw -> subscribedContracts(gw.getDescription().getChannelType(), query))
				.flatMap(List::stream)
				.toList();
	}
	
	public Map<ChannelType, Collection<ComponentField>> gatewaySettingsMeta() {
		return gatewayMetaProvider.getSettings();
	}
	
	public List<StrategyMetaInfo> strategySettingsMeta() {
		List<StrategyMetaInfo> result = new ArrayList<>();
		Map<String, Object> objMap = ctx.getBeansWithAnnotation(StrategicComponent.class);
		for (Entry<String, Object> e : objMap.entrySet()) {
			DynamicParamsAware aware = (DynamicParamsAware) e.getValue();
			StrategyMetaInfo meta = StrategyMetaInfo.builder()
					.name(e.getKey())
					.className(e.getValue().getClass().getName())
					.settings(aware.getDynamicParams().getMetaInfo())
					.build();
			result.add(meta);
		}
		return result;
	}
	
	public List<GatewayTypeDescription> channelsMeta(){
		return gatewayMetaProvider.availableChannel()
				.stream()
				.map(GatewayTypeDescription::new)
				.toList();
	}

	public List<NorthstarEventType> notifiableEvents() {
		return List.of(NorthstarEventType.LOGGED_IN, NorthstarEventType.LOGGED_OUT, NorthstarEventType.ORDER, 
				NorthstarEventType.TRADE, NorthstarEventType.NOTICE);
	}

	public Map<String, Collection<ComponentField>> msgSenderSettingsMeta() {
		return msgSenderMetaProvider.msgSenderSettingsMeta();
	}

}
