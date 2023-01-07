package tech.quantit.northstar.common.constant;

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
			return new GatewayUsage[] {GatewayUsage.MARKET_DATA};
		}
	},
	
	BIAN {
		@Override
		public GatewayUsage[] usage() {
			return new GatewayUsage[] {GatewayUsage.MARKET_DATA};
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
	};

	public abstract GatewayUsage[] usage();

	
	public boolean adminOnly() {
		return false;
	}

	public boolean allowDuplication() {
		return false;
	}
	
}
