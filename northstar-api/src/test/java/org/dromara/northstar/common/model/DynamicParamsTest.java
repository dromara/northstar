package org.dromara.northstar.common.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Objects;

import org.dromara.northstar.common.constant.FieldType;
import org.junit.jupiter.api.Test;

import lombok.Data;

class DynamicParamsTest {

	@Test
	void testResolveFromSource() throws Exception {
		DynamicParams params = new InitParams();
		DynamicParams params0 = new InitParams();
		Map<String, ComponentField> resultMap = params.getMetaInfo();
		resultMap.get("actionInterval").setValue("60");
		resultMap.get("showHedge").setValue("false");
		assertThat(params.resolveFromSource(resultMap)).isEqualTo(params0);
	}

	@Test
	void testGetMetaInfo() {
		DynamicParams params = new InitParams();
		
		Map<String, ComponentField> resultMap = params.getMetaInfo();
		assertThat(resultMap).hasSize(3);
		assertThat(resultMap.get("actionInterval").getValue()).isEqualTo(60);
		assertThat(resultMap.get("priceType").getValue()).isEqualTo("OPP_PRICE");
		assertThat(resultMap.get("showHedge").getValue()).isEqualTo(false);
	}
	
	@Data
	public static class InitParams extends DynamicParams {			// 每个策略都要有一个用于定义初始化参数的内部类，类名称不能改
		
		@Setting(label="操作间隔", type = FieldType.NUMBER, order = 10, unit = "秒")		// Label注解用于定义属性的元信息。可以声明单位
		private int actionInterval = 60;						// 属性可以为任意多个，当元素为多个时order值用于控制前端的显示顺序
		
		@Setting(label="锁仓演示", type = FieldType.SELECT, options = {"启用","禁用"}, optionsVal = {"true","false"}, order = 20)
		private boolean showHedge;
		
		@Setting(label="价格类型", type = FieldType.SELECT, options = {"市价","对手价","排队价"}, optionsVal = {"ANY_PRICE", "OPP_PRICE", "WAITING_PRICE"}, order = 30)
		private String priceType = "OPP_PRICE";

		@Override
		public int hashCode() {
			return Objects.hash(actionInterval, priceType, showHedge);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			InitParams other = (InitParams) obj;
			return actionInterval == other.actionInterval && Objects.equals(priceType, other.priceType)
					&& showHedge == other.showHedge;
		}
	
	}

}
