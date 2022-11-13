package tech.quantit.northstar.gateway.playback.ticker;

/**
 * TICK基数
 * @author KevinHuangwl
 *
 */
public record TickEntry(double price, long volume, double openInterestDelta, long timestamp) {

	public static TickEntry of(double price, long volume, double openInterestDelta, long timestamp) {
		return new TickEntry(price, volume, openInterestDelta, timestamp);
	}
}
