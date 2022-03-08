package tech.quantit.northstar.common.utils;

import java.time.LocalTime;

import tech.quantit.northstar.common.constant.TickType;

public interface MarketTimeUtil {

	TickType resolveTickType(String symbol, LocalTime time);
}
