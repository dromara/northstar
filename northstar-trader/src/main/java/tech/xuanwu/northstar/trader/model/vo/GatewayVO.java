package tech.xuanwu.northstar.trader.model.vo;

import java.io.Serializable;

import org.springframework.beans.BeanUtils;

import lombok.Data;
import xyz.redtorch.pb.CoreEnum.ConnectStatusEnum;
import xyz.redtorch.pb.CoreEnum.GatewayAdapterTypeEnum;
import xyz.redtorch.pb.CoreEnum.GatewayTypeEnum;
import xyz.redtorch.pb.CoreField.GatewayField;

@Data
public class GatewayVO implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 2069183144210664738L;

	private String gatewayId;
	
	private String name;
	
	private GatewayTypeEnum gatewayType;
	
	private GatewayAdapterTypeEnum gatewayAdapterType;
	
	private ConnectStatusEnum status;
	
	private boolean authErrorFlag;
	
	
	public static GatewayVO convertFrom(GatewayField gateway) {
		GatewayVO vo = new GatewayVO();
		BeanUtils.copyProperties(gateway.toBuilder(), vo);
		return vo;
	}
}
