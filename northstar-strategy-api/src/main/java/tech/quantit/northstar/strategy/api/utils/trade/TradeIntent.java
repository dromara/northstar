package tech.quantit.northstar.strategy.api.utils.trade;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import tech.quantit.northstar.common.TickDataAware;
import tech.quantit.northstar.common.TransactionAware;
import tech.quantit.northstar.common.constant.SignalOperation;
import tech.quantit.northstar.common.utils.OrderUtils;
import tech.quantit.northstar.strategy.api.IModuleStrategyContext;
import tech.quantit.northstar.strategy.api.constant.PriceType;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;


/**
 * 交易意图
 * 封装自动撤单追单逻辑
 * @author KevinHuangwl
 *
 */
public class TradeIntent implements TransactionAware, TickDataAware {
	
	@Setter
	private IModuleStrategyContext context;
	/**
	 * 合约
	 */
	@Getter
	@NonNull
	private final ContractField contract;
	/**
	 * 操作
	 */
	@Getter
	@NonNull
	private final SignalOperation operation;
	/**
	 * 价格类型
	 */
	@NonNull
	@Getter
	private final PriceType priceType;
	/**
	 * 价格
	 */
	@Getter
	private final double price;
	/**
	 * 目标手数
	 */
	@Getter
	private final int volume;
	/**
	 * 订单超时（毫秒）
	 */
	private final long timeout;
	/**
	 * 放弃条件，例如价格与初始值差异过大
	 */
	private final Predicate<TickField> abortCondition;
	
	@Builder
	public TradeIntent(ContractField contract, SignalOperation operation, PriceType priceType, double price, int volume, 
			long timeout, Predicate<TickField> abortCondition) {
		Assert.noNullElements(List.of(contract, operation, priceType), "入参不能为空");
		Assert.isTrue(volume > 0, "手数必须为正整数");
		this.contract = contract;
		this.operation = operation;
		this.priceType = priceType;
		this.price = price;
		this.volume = volume;
		this.timeout = timeout;
		this.abortCondition = abortCondition;
	}
	
	private Optional<String> orderIdRef = Optional.empty();

	private int accVol;
	
	private boolean terminated;
	
	private long lastCancelReqTime;
	
	@Override
	public synchronized void onTick(TickField tick) {
		if(!StringUtils.equals(tick.getUnifiedSymbol(), contract.getUnifiedSymbol())) 
			return;

		if(Objects.nonNull(abortCondition))
			terminated = abortCondition.test(tick);
		if(hasTerminated())
			return;
		if(orderIdRef.isEmpty()) {
			orderIdRef = context.submitOrderReq(contract, operation, priceType, volume - accVol, price);
		} else if (context.isOrderWaitTimeout(orderIdRef.get(), timeout) && System.currentTimeMillis() - lastCancelReqTime > 3000) {
			context.cancelOrder(orderIdRef.get());
			lastCancelReqTime = System.currentTimeMillis();
		}
	}

	@Override
	public synchronized void onOrder(OrderField order) {
		// 订单结束
		orderIdRef
			.filter(id -> StringUtils.equals(id, order.getOriginOrderId()))
			.ifPresent(id -> {
				if(OrderUtils.isDoneOrder(order)) {	
					terminated = true;
					orderIdRef = Optional.empty();
				}
			});
	}

	@Override
	public synchronized void onTrade(TradeField trade) {
		accVol += trade.getVolume();
	}

	public boolean hasTerminated() {
		return terminated || accVol == volume;
	}
}
