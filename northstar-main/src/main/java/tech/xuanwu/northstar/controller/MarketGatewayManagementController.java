package tech.xuanwu.northstar.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.xuanwu.northstar.common.model.GatewayInfo;

@RequestMapping("/market")
@RestController
public class MarketGatewayManagementController {

	@PostMapping("/gateway")
	public boolean create() {
		return true;
	}
	
	@DeleteMapping("/gateway")
	public boolean remove() {
		return true;
	}
	
	@PutMapping("/gateway")
	public boolean modify() {
		return true;
	}
	
	@GetMapping("/gateway")
	public List<GatewayInfo> list() { 
		
		return new ArrayList();
	}
}
