package tech.xuanwu.northstar.common.utils;

import java.time.LocalTime;

import tech.xuanwu.northstar.common.constant.TickType;

public interface MarketTimeUtil {

	TickType resolveTickType(LocalTime time);
}
