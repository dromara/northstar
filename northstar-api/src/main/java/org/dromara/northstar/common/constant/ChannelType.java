package org.dromara.northstar.common.constant;

public enum ChannelType {
	
	PLAYBACK {
		@Override
		public GatewayUsage[] usage() {
			return new GatewayUsage[] {GatewayUsage.MARKET_DATA};
		}

		@Override
		public boolean allowDuplication() {
			return true;
		}
	},
	
	SIM {
		@Override
		public GatewayUsage[] usage() {
			return new GatewayUsage[] {GatewayUsage.MARKET_DATA, GatewayUsage.TRADE};
		}
	},
	
	CTP {
		@Override
		public GatewayUsage[] usage() {
			return new GatewayUsage[] {GatewayUsage.MARKET_DATA, GatewayUsage.TRADE};
		}
	},
	
	TIGER {
		@Override
		public GatewayUsage[] usage() {
			return new GatewayUsage[] {GatewayUsage.MARKET_DATA, GatewayUsage.TRADE};
		}
	},

	CTP_SIM {
		@Override
		public GatewayUsage[] usage() {
			return new GatewayUsage[] {GatewayUsage.MARKET_DATA, GatewayUsage.TRADE};
		}

		@Override
		public boolean adminOnly() {
			return true;
		}
	},
	OKX {
		@Override
		public GatewayUsage[] usage() {
			return new GatewayUsage[] {GatewayUsage.MARKET_DATA, GatewayUsage.TRADE};
		}
	},
	
	BIAN {
		@Override
		public GatewayUsage[] usage() {
			return new GatewayUsage[] {GatewayUsage.MARKET_DATA, GatewayUsage.TRADE};
		}
		@Override
		public boolean allowDuplication() {
			return true;
		}
	};
	public abstract GatewayUsage[] usage();

	/**
	 * 是否只有超级用户模式才能使用
	 * @return
	 */
	public boolean adminOnly() {
		return false;
	}

	/**
	 * 是否允许多个行情网关
	 * @return
	 */
	public boolean allowDuplication() {
		return false;
	}
	
}
