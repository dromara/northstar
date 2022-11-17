package tech.quantit.northstar.gateway.playback.ticker;

/**
 * TICK基数
 * @author KevinHuangwl
 *
 */
public record TickEntry(double price, double askPrice0, double bidPrice0, long volume, double openInterestDelta, long timestamp) {

	public static TickEntry of(double price, double askPrice0, double bidPrice0, long volume, double openInterestDelta, long timestamp) {
		return new TickEntry(price, askPrice0, bidPrice0, volume, openInterestDelta, timestamp);
	}
}
