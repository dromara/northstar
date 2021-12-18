package tech.quantit.northstar.strategy.api;

public interface ExternalSignalPolicy extends SignalPolicy {

	void onExtMsg(String text);
}
