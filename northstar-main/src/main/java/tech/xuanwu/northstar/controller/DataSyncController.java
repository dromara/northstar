package tech.xuanwu.northstar.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.xuanwu.northstar.controller.common.ResultBean;
import tech.xuanwu.northstar.service.DataSyncService;

@RestController
public class DataSyncController {
	
	@Autowired
	private DataSyncService service;

	@GetMapping("/sync")
	public ResultBean<Void> sync() throws Exception {
		service.asyncUpdateContracts();
		service.asyncUpdateTradeAccount();
		return new ResultBean<>(null);
	}
	
}
