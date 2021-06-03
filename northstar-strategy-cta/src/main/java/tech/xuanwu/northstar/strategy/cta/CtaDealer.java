package tech.xuanwu.northstar.strategy.cta;

import tech.xuanwu.northstar.strategy.common.Dealer;
import tech.xuanwu.northstar.strategy.common.DynamicParams;

public class CtaDealer implements Dealer{
	
	public CtaDealer(CtaDealerParams params) {
		
	}

	
	public class CtaDealerParams extends DynamicParams<CtaDealerParams>{

		@Override
		public CtaDealerParams resolveFromSource() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}
