package tech.quantit.northstar.strategy.api.policy.risk;


import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.strategy.api.EventDrivenComponent;
import tech.quantit.northstar.strategy.api.RiskControlRule;
import tech.quantit.northstar.strategy.api.TickDataAware;
import tech.quantit.northstar.strategy.api.annotation.Setting;
import tech.quantit.northstar.strategy.api.annotation.StrategicComponent;
import tech.quantit.northstar.strategy.api.constant.RiskAuditResult;
import tech.quantit.northstar.strategy.api.event.ModuleEvent;
import tech.quantit.northstar.strategy.api.event.ModuleEventBus;
import tech.quantit.northstar.strategy.api.event.ModuleEventType;
import tech.quantit.northstar.strategy.api.model.DynamicParams;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;


/**
 * 当价格超过限制时，会拒绝继续下单
 * @author KevinHuangwl
 *
 */
@Slf4j
@StrategicComponent("委托超价限制")
public class PriceExceededRule extends AbstractRule implements RiskControlRule, TickDataAware, EventDrivenComponent {

	protected int priceDifTolerance;
	
	private SubmitOrderReqField orderReq;
	
	private ModuleEventBus meb;
	
	@Override
	public RiskAuditResult checkRisk(SubmitOrderReqField orderReq, TickField tick) {
		this.orderReq = orderReq;
		return RiskAuditResult.ACCEPTED;
	}
	
	@Override
	public void onTick(TickField tick) {
		if(orderReq != null && tick.getUnifiedSymbol().equals(orderReq.getContract().getUnifiedSymbol()) 
				&& Math.abs(tick.getLastPrice() - orderReq.getPrice()) > priceDifTolerance) {
			log.warn("[{}] 当前市价 [{}] 距离委托价 [{}] 超过风控限制 [{}]，放弃订单", getModuleName(), tick.getLastPrice(), orderReq.getPrice(), priceDifTolerance);
			meb.post(new ModuleEvent<>(ModuleEventType.REJECT_RISK_ALERTED, orderReq));
			orderReq = null;
		}
	}
	
	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		InitParams initParams = (InitParams) params;
		this.priceDifTolerance = initParams.priceDifTolerance;
	}
	
	public static class InitParams extends DynamicParams{
		
		@Setting(value="超价限制")
		private int priceDifTolerance;
		
	}

	@Override
	public void onEvent(ModuleEvent<?> moduleEvent) {
		// 不处理
	}

	@Override
	public void setEventBus(ModuleEventBus moduleEventBus) {
		meb = moduleEventBus;
	}

}
