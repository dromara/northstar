package tech.xuanwu.northstar.main.restful;

import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import tech.xuanwu.northstar.common.model.ResultBean;
import tech.xuanwu.northstar.main.service.ModuleService;
import tech.xuanwu.northstar.strategy.api.model.ComponentField;
import tech.xuanwu.northstar.strategy.api.model.ComponentMetaInfo;
import tech.xuanwu.northstar.strategy.api.model.ModuleDealRecord;
import tech.xuanwu.northstar.strategy.api.model.ModuleInfo;
import tech.xuanwu.northstar.strategy.api.model.ModulePositionInfo;
import tech.xuanwu.northstar.strategy.api.model.ModuleRealTimeInfo;
import tech.xuanwu.northstar.strategy.api.model.ModuleTradeRecord;
import tech.xuanwu.northstar.strategy.api.model.TimeSeriesData;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

@RestController
public class ModuleController {
	
	@Autowired
	private ModuleService service;
	
	/**
	 * 查询所有定义的信号策略
	 * @return
	 */
	@GetMapping("/signal/policies")
	public ResultBean<List<ComponentMetaInfo>> getRegisteredSignalPolicies(){
		
		return new ResultBean<>(service.getRegisteredSignalPolicies());
	}
	
	/**
	 * 查询所有定义的风控策略
	 * @return
	 */
	@GetMapping("/riskControl/rules")
	public ResultBean<List<ComponentMetaInfo>> getRegisteredRiskControlRules(){
		return new ResultBean<>(service.getRegisteredRiskControlRules());
	}
	
	/**
	 * 查询所有定义的交易策略
	 * @return
	 */
	@GetMapping("/trade/dealers")
	public ResultBean<List<ComponentMetaInfo>> getRegisteredDealers(){
		return new ResultBean<>(service.getRegisteredDealers());
	}
	
	/**
	 * 查询策略组件的参数设置
	 * @param info
	 * @return
	 * @throws ClassNotFoundException
	 */
	@PostMapping("/component/params")
	public ResultBean<Map<String, ComponentField>> getComponentParams(@NotNull @RequestBody ComponentMetaInfo info) throws ClassNotFoundException{
		return new ResultBean<>(service.getComponentParams(info));
	}
	
	/**
	 * 创建模组
	 * @param module
	 * @return
	 * @throws Exception
	 */
	@PostMapping("/module")
	public ResultBean<Boolean> createModule(@NotNull @RequestBody ModuleInfo module) throws Exception{
		return new ResultBean<>(service.createModule(module));
	}
	
	/**
	 * 更新模组
	 * @param module
	 * @return
	 * @throws Exception
	 */
	@PutMapping("/module")
	public ResultBean<Boolean> updateModule(@NotNull @RequestBody ModuleInfo module) throws Exception{
		return new ResultBean<>(service.updateModule(module));
	}
	
	/**
	 * 获取所有模组
	 * @return
	 */
	@GetMapping("/module")
	public ResultBean<List<ModuleInfo>> getAllModules(){
		return new ResultBean<>(service.getCurrentModuleInfos());
	}
	
	/**
	 * 删除模组
	 * @param name
	 * @return
	 */
	@DeleteMapping("/module")
	public ResultBean<Void> removeModule(@NotNull String name){
		service.removeModule(name);
		return new ResultBean<>(null);
	}
	
	/**
	 * 获取模组状态信息
	 * @param name
	 * @return
	 */
	@GetMapping("/module/info")
	public ResultBean<ModuleRealTimeInfo> getModuleRealTimeInfo(@NotNull String name){
		return new ResultBean<>(service.getModuleRealTimeInfo(name));
	}
	
	/**
	 * 透视模组引用数据
	 * @param name
	 * @return
	 */
	@GetMapping("/module/refdata")
	public ResultBean<List<TimeSeriesData>> inspectModuleDataRef(@NotNull String name){
		return new ResultBean<>(service.getModuleDataRef(name));
	}
	
	/**
	 * 获取模组交易记录
	 * @param name
	 * @return
	 */
	@GetMapping("/module/records")
	public ResultBean<List<ModuleDealRecord>> getDealRecords(@NotNull String name){
		return new ResultBean<>(service.getDealRecords(name));
	}
	
	/**
	 * 获取模组成交记录
	 * @param name
	 * @return
	 */
	@GetMapping("/module/records/trade")
	public ResultBean<List<ModuleTradeRecord>> getTradeRecords(@NotNull String name){
		return new ResultBean<>(service.getTradeRecords(name));
	}
	
	/**
	 * 模组启停状态切换
	 * @param name
	 * @return
	 */
	@GetMapping("/module/toggle")
	public ResultBean<Boolean> toggleModuleState(@NotNull String name){
		return new ResultBean<>(service.toggleState(name));
	}
	
	/**
	 * 增加模组持仓
	 * @param moduleName
	 * @param position
	 * @return
	 */
	@PostMapping("/module/{moduleName}/position")
	public ResultBean<Boolean> createPosition(@PathVariable String moduleName, @RequestBody ModulePositionInfo position){
		return new ResultBean<>(service.createPosition(moduleName, position));
	}
	
	/**
	 * 修改模组持仓
	 * @param moduleName
	 * @param position
	 * @return
	 */
	@PutMapping("/module/{moduleName}/position")
	public ResultBean<Boolean> updatePosition(@PathVariable String moduleName, @RequestBody ModulePositionInfo position){
		return new ResultBean<>(service.updatePosition(moduleName, position));
	}
	
	/**
	 * 移除模组持仓
	 * @param moduleName
	 * @param unifiedSymbol
	 * @param dir
	 * @return
	 */
	@DeleteMapping("/module/{moduleName}/position")
	public ResultBean<Boolean> removePosition(@PathVariable String moduleName, String unifiedSymbol, PositionDirectionEnum dir){
		return new ResultBean<>(service.removePosition(moduleName, unifiedSymbol, dir));
	}
}
