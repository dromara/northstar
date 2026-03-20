package org.dromara.northstar.app.service;

import java.util.List;
import java.util.Objects;

import org.dromara.northstar.common.model.CustomIndicatorVO;
import org.dromara.northstar.data.jdbc.CustomIndicatorRepository;
import org.dromara.northstar.data.jdbc.entity.CustomIndicatorDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

/**
 * 自定义指标服务类
 */
@Slf4j
@Service
public class CustomIndicatorService {

	@Autowired
	private CustomIndicatorRepository repository;

	/**
	 * 获取所有指标
	 */
	public List<CustomIndicatorVO> findAll() {
		return repository.findAllByOrderByCreatedAtAsc().stream()
				.map(CustomIndicatorDO::convertTo)
				.toList();
	}

	/**
	 * 根据系列获取指标
	 */
	public List<CustomIndicatorVO> findBySeries(String series) {
		CustomIndicatorDO.IndicatorSeries indicatorSeries = CustomIndicatorDO.IndicatorSeries.valueOf(series.toUpperCase());
		return repository.findBySeries(indicatorSeries).stream()
				.map(CustomIndicatorDO::convertTo)
				.toList();
	}

	/**
	 * 根据ID获取指标
	 */
	public CustomIndicatorVO findById(String id) {
		CustomIndicatorDO entity = repository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("指标不存在: " + id));
		return entity.convertTo();
	}

	/**
	 * 创建指标
	 */
	@Transactional
	public CustomIndicatorVO create(CustomIndicatorVO vo) {
		// 检查简称是否已存在
		if (repository.existsByShortName(vo.getShortName())) {
			throw new EntityExistsException("指标简称已存在: " + vo.getShortName());
		}

		long now = System.currentTimeMillis();
		vo.setId(generateId());
		vo.setFavorite(Boolean.FALSE);
		vo.setCreatedAt(now);
		vo.setUpdatedAt(now);

		CustomIndicatorDO entity = CustomIndicatorDO.convertFrom(vo);
		CustomIndicatorDO saved = repository.save(entity);

		log.info("创建自定义指标 [{}]", vo.getShortName());
		return saved.convertTo();
	}

	/**
	 * 更新指标
	 */
	@Transactional
	public CustomIndicatorVO update(String id, CustomIndicatorVO vo) {
		CustomIndicatorDO existing = repository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("指标不存在: " + id));

		// 如果修改了简称，检查新简称是否已存在
		if (!Objects.equals(existing.getShortName(), vo.getShortName())) {
			if (repository.existsByShortName(vo.getShortName())) {
				throw new EntityExistsException("指标简称已存在: " + vo.getShortName());
			}
		}

		existing.setShortName(vo.getShortName());
		existing.setDescription(vo.getDescription());
		existing.setSeries(CustomIndicatorDO.IndicatorSeries.valueOf(vo.getSeries().toUpperCase()));
		existing.setIndicatorTemplateCode(vo.getIndicatorTemplateCode());
		existing.setUpdatedAt(System.currentTimeMillis());

		CustomIndicatorDO saved = repository.save(existing);
		log.info("更新自定义指标 [{}]", vo.getShortName());
		return saved.convertTo();
	}

	/**
	 * 删除指标
	 */
	@Transactional
	public void delete(String id) {
		CustomIndicatorDO entity = repository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("指标不存在: " + id));
		repository.deleteById(id);
		log.info("删除自定义指标 [{}]", entity.getShortName());
	}

	/**
	 * 切换收藏状态
	 */
	@Transactional
	public CustomIndicatorVO setFavorite(String id, boolean favorite) {
		CustomIndicatorDO entity = repository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("指标不存在: " + id));
		entity.setFavorite(favorite);
		entity.setUpdatedAt(System.currentTimeMillis());
		CustomIndicatorDO saved = repository.save(entity);
		log.info("{}自定义指标 [{}] 的收藏状态", favorite ? "设置" : "取消", entity.getShortName());
		return saved.convertTo();
	}

	/**
	 * 生成唯一ID
	 */
	private String generateId() {
		return "custom_" + System.currentTimeMillis() + "_" + 
				Math.random() * 1000000000;
	}
}
