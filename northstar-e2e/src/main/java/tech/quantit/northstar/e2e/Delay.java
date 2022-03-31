package tech.quantit.northstar.e2e;

public class Delay {

	public static void with(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
