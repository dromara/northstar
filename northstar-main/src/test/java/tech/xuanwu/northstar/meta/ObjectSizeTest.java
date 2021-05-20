package tech.xuanwu.northstar.meta;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.util.RamUsageEstimator;

import tech.xuanwu.northstar.persistence.po.MinBarDataPO;
import tech.xuanwu.northstar.persistence.po.TickDataPO;

public class ObjectSizeTest {

	public static void main(String[] args) {
		List<TickDataPO> tickList = new ArrayList<>();
		for(int i=0; i<120; i++) {
			tickList.add(new TickDataPO());
		}
		MinBarDataPO po = new MinBarDataPO();
		System.out.println(RamUsageEstimator.humanSizeOf(po));
	}
}
