package tech.quantit.northstar.strategy.api;

public interface IDisposablePriceListener {
	
	void invalidate();

	void setCallback(Runnable callback);
}
