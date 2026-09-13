package org.dromara.northstar.trader;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.TransactionAware;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.LineTraderDTO;
import org.dromara.northstar.common.model.LineTraderDTO.State;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.strategy.IAccount;

import com.google.common.util.concurrent.AtomicDouble;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

@Slf4j
@Builder
public class LineTrader implements TickDataAware, TransactionAware {

	@Getter
	private LineTraderDTO dto;

	private AccountManager accountManager;

	private MarketCenter marketCenter;

	private Tick lastTick;

	private Consumer<LineTraderDTO> stateChangeCallback;
	
	@Builder.Default
	private Optional<String> orderId = Optional.empty();
	
	private long orderCompletedTime;
	
	@Override
	public void onOrder(Order order) {
		orderId.ifPresent(id -> {
			if(order.originOrderId().equals(id) && OrderUtils.isValidOrder(order) && OrderUtils.isDoneOrder(order)) {
				orderId = Optional.empty();
				orderCompletedTime = System.currentTimeMillis();
			}
		});
	}

	@Override
	public void onTrade(Trade trade) {
		// 不响应
	}
	
	@Override
	public void onTick(Tick tick) {
		if (!tick.contract().contractId().equals(dto.getContractId()) || dto.getState() == State.COMPLETED) {
			return;
		}

		// 处理未入场的交易计划
		if (dto.getOrderPrice() != null && dto.getState() == State.STANDBY) {
			if (lastTick != null && crossPrice(tick, dto.getOrderPrice()) 
					&& ("long".equals(dto.getPositionDir()) && tick.lastPrice() > dto.getStopLossPrice() && tick.lastPrice() < dto.getTakeProfitPrice()
					|| "short".equals(dto.getPositionDir()) && tick.lastPrice() < dto.getStopLossPrice() && tick.lastPrice() > dto.getTakeProfitPrice())) {
				log.info("画线交易计划触发【入场】操作：{} {} 计划入场价：{} 现价：{} -> {}", 
						tick.contract().name(), dto.getPositionDir(), dto.getOrderPrice(), lastTick.lastPrice(), tick.lastPrice());
				doEntryTrade(tick);
			}
			lastTick = tick;
			return;
		}

		if(hasPosition()) {			
			if(dto.getOrderPrice() == null && dto.getState() == State.STANDBY) {
				dto.setState(State.RUNNING);
				stateChangeCallback.accept(dto);
			}
			// 处理止盈止损
			switch (dto.getPositionDir()) {
				case "long" -> {
					// 处理止盈
					if (dto.getTakeProfitPrice() != null && tick.lastPrice() >= dto.getTakeProfitPrice()) {
						log.info("画线交易计划触发【多头止盈】操作：{} 止盈：{} 现价：{}", tick.contract().name(), dto.getTakeProfitPrice(), tick.lastPrice());
						doExitTrade(tick);
					}
					
					// 处理止损
					if (dto.getStopLossPrice() != null && tick.lastPrice() <= dto.getStopLossPrice()) {
						log.info("画线交易计划触发【多头止损】操作：{} 止损：{} 现价：{}", tick.contract().name(), dto.getStopLossPrice(), tick.lastPrice());
						doExitTrade(tick);
					}
					
				}
				case "short" -> {
					// 处理止盈
					if (dto.getTakeProfitPrice() != null && tick.lastPrice() <= dto.getTakeProfitPrice()) {
						log.info("画线交易计划触发【空头止盈】操作：{} 止盈：{} 现价：{}", tick.contract().name(), dto.getTakeProfitPrice(), tick.lastPrice());
						doExitTrade(tick);
					}
					
					// 处理止损
					if (dto.getStopLossPrice() != null && tick.lastPrice() >= dto.getStopLossPrice()) {
						log.info("画线交易计划触发【空头止损】操作：{} 止损：{} 现价：{}", tick.contract().name(), dto.getStopLossPrice(), tick.lastPrice());
						doExitTrade(tick);
					}
				}
			}
		} else if(orderId.isEmpty() && System.currentTimeMillis() - orderCompletedTime > 1000) {
			dto.setState(State.COMPLETED);
			stateChangeCallback.accept(dto);
		}

		lastTick = tick;
	}
	
	// 入场操作
	private void doEntryTrade(Tick tick) {
		Contract c = marketCenter.getContract(Identifier.of(dto.getContractId())).contract();
		DirectionEnum direction = switch(dto.getPositionDir()) {
		case "long" -> DirectionEnum.D_Buy;
		case "short" ->  DirectionEnum.D_Sell;
		default -> throw new IllegalArgumentException("未知方向：" + dto.getPositionDir());
		};
		
		SubmitOrderReq orderReq = SubmitOrderReq.builder()
				.gatewayId(dto.getAccountGatewayId())
				.originOrderId(String.valueOf(System.currentTimeMillis()))
				.contract(c)
				.direction(direction)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.volume(dto.getVolume())
				.price(rounding(dto.getOrderPrice(), c))	// 自动加上超价
				.build();
		log.info("{} {} {} 画线交易执行开仓操作：方向{}, {}, {}", tick.actionDay(), tick.actionTime(), c.name(), orderReq.direction(),
				orderReq.price(), orderReq.volume());
		IAccount account = accountManager.get(Identifier.of(dto.getAccountGatewayId()));
		account.submitOrder(orderReq);
		orderId = Optional.of(orderReq.originOrderId());
		dto.setState(State.RUNNING);
		stateChangeCallback.accept(dto);
	}

	// 离场操作
	private void doExitTrade(Tick tick) {
		Contract c = marketCenter.getContract(Identifier.of(dto.getContractId())).contract();
		IAccount account = accountManager.get(Identifier.of(dto.getAccountGatewayId()));
		
		DirectionEnum direction = null;
		AtomicDouble volume = new AtomicDouble();
		AtomicReference<Position> positionRef = new AtomicReference<>();
		switch(dto.getPositionDir()) {
		case "long" -> {
			direction = DirectionEnum.D_Sell;
			account.getPosition(PositionDirectionEnum.PD_Long, c).ifPresent(p -> {
				volume.set(Math.min(dto.getVolume(), p.position() - p.frozen()));
				positionRef.set(p);
			});
		}
		case "short" ->  {
			direction = DirectionEnum.D_Buy;
			account.getPosition(PositionDirectionEnum.PD_Short, c).ifPresent(p -> {
				volume.set(Math.min(dto.getVolume(), p.position() - p.frozen()));
				positionRef.set(p);
			});
		}
		default -> throw new IllegalArgumentException("未知方向：" + dto.getPositionDir());
		};
		
		SubmitOrderReq orderReq = SubmitOrderReq.builder()
				.gatewayId(dto.getAccountGatewayId())
				.originOrderId(UUID.randomUUID().toString())
				.contract(c)
				.direction(direction)
				.offsetFlag(OffsetFlagEnum.OF_Close)
				.volume(volume.get())
				.build();
		log.info("{} {} {} 画线交易执行平仓操作：{}, {}, {}", tick.actionDay(), tick.actionTime(), c.name(), orderReq.direction(), orderReq.price(), 
				orderReq.volume());
		account.submitOrder(orderReq);
		
		dto.setState(State.COMPLETED);
		
		stateChangeCallback.accept(dto);
	}
	
	public boolean hasPosition() {
		Contract c = marketCenter.getContract(Identifier.of(dto.getContractId())).contract();
		IAccount account = accountManager.get(Identifier.of(dto.getAccountGatewayId()));
		PositionDirectionEnum direction = switch(dto.getPositionDir()) {
			case "long" -> PositionDirectionEnum.PD_Long;
			case "short" ->  PositionDirectionEnum.PD_Short;
			default -> throw new IllegalArgumentException("未知方向：" + dto.getPositionDir());
		};
		Optional<Position> position =  account.getPosition(direction, c);
		return position.isPresent() && position.get().position() > 0;
	}
	
	private double rounding (double val, Contract c) {
	    int numOfTicks = (int) (Math.round(val / c.priceTick()));
	    return numOfTicks * c.priceTick();
	}
	
	private boolean crossPrice(Tick tick, double price) {
		return (lastTick.lastPrice() - price) * (tick.lastPrice() - price) < 0;
	}

}
