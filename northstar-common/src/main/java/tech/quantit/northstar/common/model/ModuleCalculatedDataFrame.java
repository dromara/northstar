package tech.quantit.northstar.common.model;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

/**
 * 模组数据帧
 * @author Administrator
 *
 */
@Data
public class ModuleCalculatedDataFrame {

	private long timestamp;
	
	private Map<String, Double> priceBaseValues = new HashMap<>();
	
	private Map<String, Double> volBaseValues = new HashMap<>();
	
	private Map<String, Double> openInterestBaseValues = new HashMap<>();
}
