package tech.quantit.northstar.gateway.api;


/**
 * 订阅管理器，负责管理订阅信息
 * @author KevinHuangwl
 *
 */
public interface SubscriptionManager {

	boolean subscribable(IContract contract);
}
