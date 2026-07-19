package org.dromara.northstar.app.restful;

import java.util.List;

import org.dromara.northstar.app.service.ModuleService;
import org.dromara.northstar.common.model.ModuleChartDescription;
import org.dromara.northstar.common.model.ModuleDealRecordVO;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleMockTradeDTO;
import org.dromara.northstar.common.model.ModuleRuntimeVO;
import org.dromara.northstar.common.model.ModuleVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("northstar/module")
@RestController
public class ModuleController {

	@Autowired
	private ModuleService service;

	@PostMapping
	public ModuleVO createModule(@RequestBody ModuleDescription md) throws Exception {
		return service.createModule(md);
	}

	@PutMapping
	public ModuleVO updateModule(@RequestBody ModuleDescription md, boolean reset) throws Exception {
		return service.updateModule(md, reset);
	}

	@GetMapping
	public List<ModuleVO> getModules() {
		return service.getModules();
	}

	@DeleteMapping
	public void removeModule(String moduleName) {
		service.removeModule(moduleName);
	}

	@GetMapping("/{moduleName}/enable")
	public void enableModule(@PathVariable String moduleName) {
		service.enableModule(moduleName);
	}

	@GetMapping("/{moduleName}/disable")
	public void disableModule(@PathVariable String moduleName) {
		service.disableModule(moduleName);
	}

	@GetMapping("/{moduleName}/rt")
	public ModuleRuntimeVO getRuntime(@PathVariable String moduleName) {
		return service.getRuntime(moduleName);
	}

	@GetMapping("/{moduleName}/deals")
	public List<ModuleDealRecordVO> getDeals(@PathVariable String moduleName) {
		return service.getDealRecords(moduleName);
	}

	@GetMapping("/{moduleName}/chart")
	public ModuleChartDescription getChart(@PathVariable String moduleName) {
		return service.getChart(moduleName);
	}

	@PostMapping("/mocktrade")
	public void mockTrade(@RequestBody ModuleMockTradeDTO trade) {
		service.mockTrade(trade);
	}
}
