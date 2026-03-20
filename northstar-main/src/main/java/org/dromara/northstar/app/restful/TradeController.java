package org.dromara.northstar.app.restful;

import java.util.List;

import org.dromara.northstar.app.service.TradeService;
import org.dromara.northstar.common.model.LineTraderDTO;
import org.dromara.northstar.common.model.ModuleDealRecordVO;
import org.dromara.northstar.common.model.OrderRequestDTO;
import org.dromara.northstar.common.model.OrderWithdrawDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("northstar/trade")
@RestController
public class TradeController {
	
	@Autowired
	private TradeService service;

	/**
	 * 发单
	 * @return
	 */
	@PostMapping("order/submit")
	public void submitOrder(@RequestBody OrderRequestDTO dto){
		service.submitOrder(dto);
	}
	
	/**
	 * 撤单
	 * @return
	 */
	@PostMapping("order/cancel")
	public void cancelOrder(@RequestBody OrderWithdrawDTO dto){
		service.cancelOrder(dto);
	}
	
	/**
	 * 创建交易计划
	 * @param accountId
	 * @param contractId
	 * @return
	 */
	@PostMapping("plan")
	public void saveLineTrader(@RequestBody LineTraderDTO lineTrader) {
		service.saveLineTrader(lineTrader);
	}
	
	/**
	 * 读取交易计划
	 * @param accountId
	 * @param contractId
	 * @return
	 */
	@GetMapping("plan")
	public List<LineTraderDTO> getLineTraders(String accountId, String contractId){
		return service.getLineTraders(accountId, contractId);
	}
	
	/**
	 * 移除交易计划
	 * @param id
	 */
	@DeleteMapping("plan")
	public void removeLineTrader(String id) {
		service.removeLineTrader(id);
	}
	
	/**
	 * 发送订单与成交信息
	 */
	@GetMapping("cache/resend")
	public void resendCache(){
		service.resendOrderAndTradeCache();
	}
	
	/**
	 * 获取交易记录
	 * @param accountId
	 * @return
	 */
	@GetMapping("deals")
	public List<ModuleDealRecordVO> getDealRecords(String accountId){
		return service.getDealRecords(accountId);
	}
}
