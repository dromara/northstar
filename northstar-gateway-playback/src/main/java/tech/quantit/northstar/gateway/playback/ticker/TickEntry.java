package tech.quantit.northstar.gateway.playback.ticker;

/**
 * TICK基数
 * @author KevinHuangwl
 *
 */
public record TickEntry(double price, long volumeDelta, double openInterest, long timestamp) {

	public static TickEntry of(double price, long volumeDelta, double openInterest, long timestamp) {
		return new TickEntry(price, volumeDelta, openInterest, timestamp);
	}
}
