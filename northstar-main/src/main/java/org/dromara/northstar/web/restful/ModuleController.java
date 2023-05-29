package org.dromara.northstar.web.restful;

import java.util.List;
import java.util.Map;

import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.model.ComponentField;
import org.dromara.northstar.common.model.ComponentMetaInfo;
import org.dromara.northstar.common.model.MockTradeDescription;
import org.dromara.northstar.common.model.ModuleDealRecord;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.common.model.ResultBean;
import org.dromara.northstar.web.service.ModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.protobuf.InvalidProtocolBufferException;

@RequestMapping("/northstar/module")
@RestController
public class ModuleController {

	@Autowired
	private ModuleService service;
	
	/**
	 * 查询所有定义的信号策略
	 * @return
	 */
	@GetMapping("/strategies")
	public ResultBean<List<ComponentMetaInfo>> getRegisteredTradeStrategies(){
		return new ResultBean<>(service.getRegisteredTradeStrategies());
	}
	
	/**
	 * 查询策略组件的参数设置
	 * @param info
	 * @return
	 * @throws ClassNotFoundException
	 */
	@PostMapping("/strategy/params")
	public ResultBean<Map<String, ComponentField>> getComponentParams(@RequestBody ComponentMetaInfo info) throws ClassNotFoundException{
		Assert.notNull(info, "组件不能为空");
		return new ResultBean<>(service.getComponentParams(info));
	}
	
	/**
	 * 创建模组
	 * @param module
	 * @return		返回更新后实体
	 * @throws Exception
	 */
	@PostMapping
	public ResultBean<ModuleDescription> createModule(@RequestBody ModuleDescription module) throws Exception{
		Assert.notNull(module, "模组对象不能为空");
		return new ResultBean<>(service.createModule(module));
	}
	
	/**
	 * 更新模组
	 * @param module
	 * @return		返回更新后实体
	 * @throws Exception
	 */
	@PutMapping
	public ResultBean<ModuleDescription> updateModule(@RequestBody ModuleDescription module, boolean reset) throws Exception{
		Assert.notNull(module, "模组对象不能为空");
		return new ResultBean<>(service.modifyModule(module, reset));
	}
	
	/**
	 * 获取所有模组
	 * @return
	 */
	@GetMapping
	public ResultBean<List<ModuleDescription>> getAllModules(){
		return new ResultBean<>(service.findAllModules());
	}
	
	/**
	 * 删除模组
	 * @param name
	 * @return		返回删除结果提示
	 */
	@DeleteMapping
	public ResultBean<Boolean> removeModule(String name){
		Assert.notNull(name, "模组名称不能为空");
		return new ResultBean<>(service.removeModule(name));
	}
	
	/**
	 * 模组启停状态切换
	 * @param name
	 * @return	返回更新后状态
	 */
	@GetMapping("/toggle")
	public ResultBean<Boolean> toggleModuleState(String name){
		Assert.notNull(name, "模组名称不能为空");
		return new ResultBean<>(service.toggleModule(name));
	}
	
	/**
	 * 获取模组状态信息
	 * @param name
	 * @return
	 */
	@GetMapping("/rt/info")
	public ResultBean<ModuleRuntimeDescription> getModuleRealTimeInfo(String name){
		Assert.notNull(name, "模组名称不能为空");
		return new ResultBean<>(service.getModuleRealTimeInfo(name));
	}
	
	/**
	 * 获取模组持仓状态
	 * @param name
	 * @return
	 */
	@GetMapping("/rt/state")
	public ResultBean<ModuleState> getModuleState(String name){
		Assert.notNull(name, "模组名称不能为空");
		return new ResultBean<>(service.getModuleState(name));
	}
	
	/**
	 * 获取模组启停状态
	 * @param name
	 * @return
	 */
	@GetMapping("/rt/status")
	public ResultBean<Boolean> hasEnabled(String name){
		Assert.notNull(name, "模组名称不能为空");
		return new ResultBean<>(service.hasModuleEnabled(name));
	}
	
	/**
	 * 获取模组交易记录
	 * @param name
	 * @return
	 */
	@GetMapping("/deal/records")
	public ResultBean<List<ModuleDealRecord>> getDealRecords(String name){
		Assert.notNull(name, "模组名称不能为空");
		return new ResultBean<>(service.getDealRecords(name));
	}
	
	/**
	 * 手动调整模组持仓
	 * @param moduleName
	 * @param position
	 * @return
	 * @throws InvalidProtocolBufferException 
	 */
	@PostMapping("/{moduleName}/mockTrade")
	public ResultBean<Boolean> mockTradeAdjustment(@PathVariable String moduleName, @RequestBody MockTradeDescription mockTrade) {
		Assert.notNull(moduleName, "模组名称不能为空");
		return new ResultBean<>(service.mockTradeAdjustment(moduleName, mockTrade));
	}
	
}
