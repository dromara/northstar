package org.dromara.northstar.app.restful;

import java.util.List;
import java.util.Map;

import org.dromara.northstar.app.service.CustomIndicatorService;
import org.dromara.northstar.common.model.CustomIndicatorVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;

/**
 * 自定义指标控制器
 */
@RequestMapping("northstar/custom-indicators")
@RestController
public class CustomIndicatorController {

	@Autowired
	private CustomIndicatorService service;

	/**
	 * 获取所有自定义指标
	 */
	@GetMapping
	public List<CustomIndicatorVO> getAll(@RequestParam(required = false) String series) {
		if (series != null && !series.isEmpty()) {
			return service.findBySeries(series);
		}
		return service.findAll();
	}

	/**
	 * 获取单个指标详情
	 */
	@GetMapping("/{id}")
	public CustomIndicatorVO getById(@PathVariable String id) {
		return service.findById(id);
	}

	/**
	 * 创建自定义指标
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CustomIndicatorVO create(@RequestBody CustomIndicatorVO vo) {
		return service.create(vo);
	}

	/**
	 * 更新自定义指标
	 */
	@PutMapping("/{id}")
	public CustomIndicatorVO update(@PathVariable String id, @RequestBody CustomIndicatorVO vo) {
		return service.update(id, vo);
	}

	/**
	 * 删除自定义指标
	 */
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable String id) {
		service.delete(id);
	}

	/**
	 * 设置/取消收藏
	 */
	@PutMapping("/{id}/favorite")
	public CustomIndicatorVO setFavorite(@PathVariable String id, @RequestBody Map<String, Boolean> body) {
		Boolean favorite = body.get("favorite");
		if (favorite == null) {
			throw new IllegalArgumentException("缺少 favorite 参数");
		}
		return service.setFavorite(id, favorite);
	}

	/**
	 * 处理实体已存在异常
	 */
	@org.springframework.web.bind.annotation.ExceptionHandler(EntityExistsException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public Map<String, String> handleEntityExists(EntityExistsException e) {
		return Map.of("error", e.getMessage());
	}

	/**
	 * 处理实体不存在异常
	 */
	@org.springframework.web.bind.annotation.ExceptionHandler(EntityNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public Map<String, String> handleEntityNotFound(EntityNotFoundException e) {
		return Map.of("error", e.getMessage());
	}

	/**
	 * 处理非法参数异常
	 */
	@org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, String> handleIllegalArgument(IllegalArgumentException e) {
		return Map.of("error", e.getMessage());
	}
}
