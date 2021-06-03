package tech.xuanwu.northstar.strategy.cta.signal;

import tech.xuanwu.northstar.strategy.common.BarData;
import tech.xuanwu.northstar.strategy.common.DynamicParams;
import tech.xuanwu.northstar.strategy.common.Label;
import tech.xuanwu.northstar.strategy.common.SignalPolicy;
import xyz.redtorch.pb.CoreField.TickField;

public class SampleSignalPolicy implements SignalPolicy{
	
	/**
	 * 绑定合约
	 */
	private String bindedUnifiedSymbol;

	public SampleSignalPolicy(String unifiedSymbol, SamplePolicyParams params) {
		this.bindedUnifiedSymbol = unifiedSymbol;
		
	}
	
	@Override
	public String name() {
		return "示例策略";
	}

	@Override
	public void updateTick(TickField tick, BarData barData) {
		// TODO Auto-generated method stub
		
	}

	
	public class SamplePolicyParams extends DynamicParams<SamplePolicyParams>{
		
		@Label(value="绑定合约", order=1)
		private String unifiedSymbol;
		
		@Override
		public SamplePolicyParams resolveFromSource() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}


	@Override
	public DynamicParams<?> getDynamicParams() {
		return new SamplePolicyParams();
	}
}
