package tech.xuanwu.northstar.trader.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import tech.xuanwu.northstar.exception.NoSuchAccountException;
import tech.xuanwu.northstar.exception.NoSuchContractException;
import tech.xuanwu.northstar.gateway.GatewayApi;
import tech.xuanwu.northstar.service.ITradeService;
import tech.xuanwu.northstar.trader.constants.Constants;
import xyz.redtorch.pb.CoreEnum.ConnectStatusEnum;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.ForceCloseReasonEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.GatewayField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

/**
 * 
 * @author kevinhuangwl
 *
 */
@Service
public class TradeServiceImpl implements ITradeService{
	
	@Autowired
	@Qualifier(Constants.TRADABLE_ACCOUNT)
	private Map<String, GatewayApi> gatewayApiMap;
	
	@Autowired
	@Qualifier(Constants.CONTRACT_MAP)
	private Map<String, ContractField> contractMap;
	
	Comparator<ContractField> comparator = new Comparator<>() {

		@Override
		public int compare(ContractField o1, ContractField o2) {
			return o1.getName().compareTo(o2.getName());
		}
		
	};

	@Override
	public String submitOrder(String gatewayId, String symbol, double price, int volume, DirectionEnum dir,
			OffsetFlagEnum dealType, OrderPriceTypeEnum priceType, TimeConditionEnum timeCondition) {
		GatewayApi gatewayApi = gatewayApiMap.get(gatewayId);
		if(gatewayApi == null) {
			throw new NoSuchAccountException(gatewayId);
		}
		ContractField contract = contractMap.get(symbol);
		if(contract == null) {
			throw new NoSuchContractException(symbol);
		}
		SubmitOrderReqField submitOrderReq = SubmitOrderReqField.newBuilder()
				.setContract(contract)
				.setPrice(price)
				.setVolume(volume)
				.setOrderPriceType(priceType != null ? priceType : OrderPriceTypeEnum.OPT_LimitPrice)
				.setDirection(dir)
				.setOffsetFlag(dealType)
				.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
				.setTimeCondition(timeCondition != null ? timeCondition : TimeConditionEnum.TC_GFD)
				.setVolumeCondition(VolumeConditionEnum.VC_AV)
				.setForceCloseReason(ForceCloseReasonEnum.FCR_NotForceClose)
				.setContingentCondition(ContingentConditionEnum.CC_Immediately)
				.setMinVolume(1)
				.setGatewayId(gatewayApi.getGateway().getGatewayId())
				.build();
		return gatewayApi.submitOrder(submitOrderReq);
	}

	@Override
	public boolean cancelOrder(String gatewayId, String orderId) {
		GatewayApi gatewayApi = gatewayApiMap.get(gatewayId);
		if(gatewayApi == null) {
			throw new NoSuchAccountException(gatewayId);
		}
		CancelOrderReqField cancelOrderReq = CancelOrderReqField.newBuilder()
				.setOrderId(orderId)
				.build();
		return gatewayApi.cancelOrder(cancelOrderReq);
	}

	@Override
	public List<GatewayField> getTradableAccountList() {
		List<GatewayField> resultList = new ArrayList<>();
		gatewayApiMap.forEach((k,v) -> {
			GatewayField gf = v.getGateway();
			gf = gf.toBuilder()
			.setStatus(v.isConnected() ? ConnectStatusEnum.CS_Connected : ConnectStatusEnum.CS_Disconnected)
			.build();
			resultList.add(gf);
		});
		return resultList;
	}

	@Override
	public List<byte[]> getContracts() {
		int size = contractMap.size() / 2;
		List<byte[]> contractList = new ArrayList<>(size);
		List<ContractField> sortList = new ArrayList<>(size);
		Set<ContractField> distinctSet = new HashSet<>();
		for(Entry<String, ContractField> e : contractMap.entrySet()) {
			ContractField c = e.getValue();
			if(distinctSet.contains(c)) {
				continue;
			}
			distinctSet.add(c);
			sortList.add(c);
		}
		Collections.sort(sortList, comparator);
		return sortList.stream().map(item -> item.toByteArray()).collect(Collectors.toList());
	}

}
