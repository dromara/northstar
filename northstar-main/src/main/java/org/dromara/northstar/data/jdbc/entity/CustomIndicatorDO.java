package org.dromara.northstar.data.jdbc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import org.dromara.northstar.common.model.CustomIndicatorVO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 自定义指标实体类
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "CUSTOM_INDICATOR")
public class CustomIndicatorDO {

	@Id
	private String id;

	@Column(nullable = false, unique = true, length = 16)
	private String shortName;

	@Column(length = 200)
	private String description;

	@Column(nullable = false, length = 10)
	@Enumerated(EnumType.STRING)
	private IndicatorSeries series;

	@Lob
	@Column(nullable = false)
	private String indicatorTemplateCode;

	@Column(nullable = false)
	private Boolean favorite = false;

	@Column(nullable = false)
	private Long createdAt;

	@Column(nullable = false)
	private Long updatedAt;

	/**
	 * 指标系列枚举
	 */
	public enum IndicatorSeries {
		PRICE, VOLUME
	}

	public static CustomIndicatorDO convertFrom(CustomIndicatorVO vo) {
		CustomIndicatorDO entity = new CustomIndicatorDO();
		entity.setId(vo.getId());
		entity.setShortName(vo.getShortName());
		entity.setDescription(vo.getDescription());
		entity.setSeries(IndicatorSeries.valueOf(vo.getSeries().toUpperCase()));
		entity.setIndicatorTemplateCode(vo.getIndicatorTemplateCode());
		entity.setFavorite(vo.getFavorite() != null ? vo.getFavorite() : false);
		entity.setCreatedAt(vo.getCreatedAt());
		entity.setUpdatedAt(vo.getUpdatedAt());
		return entity;
	}

	public CustomIndicatorVO convertTo() {
		CustomIndicatorVO vo = new CustomIndicatorVO();
		vo.setId(this.id);
		vo.setShortName(this.shortName);
		vo.setDescription(this.description);
		vo.setSeries(this.series.name().toLowerCase());
		vo.setIndicatorTemplateCode(this.indicatorTemplateCode);
		vo.setFavorite(this.favorite);
		vo.setCreatedAt(this.createdAt);
		vo.setUpdatedAt(this.updatedAt);
		return vo;
	}
}
