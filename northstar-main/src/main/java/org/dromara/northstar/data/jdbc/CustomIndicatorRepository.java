package org.dromara.northstar.data.jdbc;

import java.util.List;

import org.dromara.northstar.data.jdbc.entity.CustomIndicatorDO;
import org.springframework.data.repository.CrudRepository;

public interface CustomIndicatorRepository extends CrudRepository<CustomIndicatorDO, String> {

	/**
	 * 根据简称查找指标
	 */
	CustomIndicatorDO findByShortName(String shortName);

	/**
	 * 根据系列查找指标
	 */
	List<CustomIndicatorDO> findBySeries(CustomIndicatorDO.IndicatorSeries series);

	/**
	 * 根据系列和收藏状态查找指标
	 */
	List<CustomIndicatorDO> findBySeriesAndFavorite(CustomIndicatorDO.IndicatorSeries series, boolean favorite);

	/**
	 * 查找所有指标，按创建时间升序
	 */
	List<CustomIndicatorDO> findAllByOrderByCreatedAtAsc();

	/**
	 * 检查简称是否已存在
	 */
	boolean existsByShortName(String shortName);
}
