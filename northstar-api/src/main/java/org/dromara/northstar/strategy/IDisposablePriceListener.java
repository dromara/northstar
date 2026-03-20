package org.dromara.northstar.strategy;

public interface IDisposablePriceListener {
	
	void invalidate();

	void setCallback(Runnable callback);
}
