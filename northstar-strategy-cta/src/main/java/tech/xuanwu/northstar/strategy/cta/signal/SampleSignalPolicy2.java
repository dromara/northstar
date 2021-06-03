package tech.xuanwu.northstar.strategy.cta.signal;

import tech.xuanwu.northstar.strategy.common.BarData;
import tech.xuanwu.northstar.strategy.common.DynamicParams;
import tech.xuanwu.northstar.strategy.common.Label;
import tech.xuanwu.northstar.strategy.common.SignalPolicy;
import xyz.redtorch.pb.CoreField.TickField;

public class SampleSignalPolicy2 implements SignalPolicy{
	
	/**
	 * 绑定合约
	 */
	private String bindedUnifiedSymbol;

	public SampleSignalPolicy2(SamplePolicyParams2 params) {
		
	}
	
	@Override
	public String name() {
		return "示例策略2";
	}

	@Override
	public void updateTick(TickField tick, BarData barData) {
		// TODO Auto-generated method stub
		
	}

	
	public class SamplePolicyParams2 extends DynamicParams<SamplePolicyParams2>{
		
		@Label(value="绑定合约", order=1)
		private String unifiedSymbol;
		
		@Label(value="短周期", order=2)
		private int shortPeriod;
		
		@Label(value="长周期", order=3)
		private int longPeriod;

		@Override
		public SamplePolicyParams2 resolveFromSource() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}


	@Override
	public DynamicParams<?> getDynamicParams() {
		return new SamplePolicyParams2();
	}
}
