package tech.quantit.northstar.main.restful;

import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

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

import tech.quantit.northstar.common.model.ComponentField;
import tech.quantit.northstar.common.model.ComponentMetaInfo;
import tech.quantit.northstar.common.model.ModuleDealRecord;
import tech.quantit.northstar.common.model.ModuleDescription;
import tech.quantit.northstar.common.model.ModuleRuntimeDescription;
import tech.quantit.northstar.common.model.ResultBean;
import tech.quantit.northstar.main.service.ModuleService;
import xyz.redtorch.pb.CoreField.TradeField;

@RequestMapping("/northstar/module")
@RestController
public class ModuleController {

	@Autowired
	private ModuleService service;
	
	/**
	 * 查询所有定义的信号策略
	 * @return
	 */
	@GetMapping("/components")
	public ResultBean<List<ComponentMetaInfo>> getRegisteredTradeStrategies(){
		return new ResultBean<>(service.getRegisteredTradeStrategies());
	}
	
	/**
	 * 查询策略组件的参数设置
	 * @param info
	 * @return
	 * @throws ClassNotFoundException
	 */
	@PostMapping("/component/params")
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
	public ResultBean<ModuleDescription> createModule(@NotNull @RequestBody ModuleDescription module) throws Exception{
		Assert.notNull(module, "模组不能为空");
		return new ResultBean<>(service.createModule(module));
	}
	
	/**
	 * 更新模组
	 * @param module
	 * @return		返回更新后实体
	 * @throws Exception
	 */
	@PutMapping
	public ResultBean<ModuleDescription> updateModule(@NotNull @RequestBody ModuleDescription module) throws Exception{
		Assert.notNull(module, "模组不能为空");
		return new ResultBean<>(service.modifyModule(module));
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
	public ResultBean<Boolean> removeModule(@NotNull String name){
		return new ResultBean<>(service.removeModule(name));
	}
	
	/**
	 * 模组启停状态切换
	 * @param name
	 * @return	返回更新后状态
	 */
	@GetMapping("/toggle")
	public ResultBean<Boolean> toggleModuleState(@NotNull String name){
		return new ResultBean<>(service.toggleModule(name));
	}
	
	/**
	 * 获取模组状态信息
	 * @param name
	 * @return
	 */
	@GetMapping("/rt/info")
	public ResultBean<ModuleRuntimeDescription> getModuleRealTimeInfo(@NotNull String name){
		return new ResultBean<>(service.getModuleRealTimeInfo(name));
	}
	
	/**
	 * 获取模组交易记录
	 * @param name
	 * @return
	 */
	@GetMapping("/deal/records")
	public ResultBean<List<ModuleDealRecord>> getDealRecords(@NotNull String name){
		return new ResultBean<>(service.getDealRecords(name));
	}
	
	/**
	 * 手动调整模组持仓
	 * @param moduleName
	 * @param position
	 * @return
	 * @throws InvalidProtocolBufferException 
	 */
	@NotNull
	@PostMapping("/{moduleName}/mockTrade")
	public ResultBean<Boolean> mockTradeAdjustment(@PathVariable String moduleName, @RequestBody byte[] mockTrade) throws InvalidProtocolBufferException{
		return new ResultBean<>(service.mockTradeAdjustment(moduleName, TradeField.parseFrom(mockTrade)));
	}
	
}
