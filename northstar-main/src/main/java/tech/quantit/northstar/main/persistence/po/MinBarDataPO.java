package tech.quantit.northstar.main.persistence.po;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 1分钟BAR数据
 * @author KevinHuangwl
 *
 */
@Document
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MinBarDataPO {
	
	private String unifiedSymbol;
	
	private String gatewayId;
	
	private long updateTime;
	
	private byte[] barData;
	
	private List<byte[]> ticksData;
}
