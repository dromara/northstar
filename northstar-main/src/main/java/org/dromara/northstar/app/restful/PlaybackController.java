package org.dromara.northstar.app.restful;

import java.util.List;

import org.dromara.northstar.app.service.PlaybackService;
import org.dromara.northstar.common.model.BarVO;
import org.dromara.northstar.common.model.PlaybackStatusVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import xyz.redtorch.pb.CoreEnum.BarCycleEnum;

@RestController
@RequestMapping("northstar/playback")
public class PlaybackController {
	
	@Autowired
	private PlaybackService service;
	
	@GetMapping("start")
	public void start() {
		service.start();
	}
	
	@GetMapping("pause")
	public void pause() {
		service.pause();
	}
	
	@GetMapping("preload")
	public void preload() {
		service.preload();
	}
	
	@GetMapping("reset")
	public void reset() {
		service.reset();
	}
	
	@PutMapping("speed/{interval}")
	public void setSpeed(@PathVariable int interval) {
		service.setSpeed(interval);
	}
	
	@GetMapping("time/{timestamp}")
	public void setTime(@PathVariable long timestamp) {
		service.setTime(timestamp);
	}
	
	@GetMapping("next/{step}")
	public void playNext(@PathVariable String step) {
		BarCycleEnum period = switch(step) {
		case "1m" -> BarCycleEnum.B_1Min;
		case "3m" -> BarCycleEnum.B_3Min;
		case "5m" -> BarCycleEnum.B_5Min;
		case "10m" -> BarCycleEnum.B_10Min;
		case "15m" -> BarCycleEnum.B_15Min;
		case "30m" -> BarCycleEnum.B_30Min;
		case "1h" -> BarCycleEnum.B_1Hr;
		case "2h" -> BarCycleEnum.B_2Hr;
		case "4h" -> BarCycleEnum.B_4Hr;
		case "1d" -> BarCycleEnum.B_1Day;
		case "1wk" -> BarCycleEnum.B_1Wk;
		default ->  throw new IllegalArgumentException("未定义参数：" + step);
		};
		service.playNext(period);
	}
	
	@GetMapping("status")
	public PlaybackStatusVO status() {
		return service.getPlaybackStatus();
	}
	
	@GetMapping("history/{contractId}/{barPeriodVal}")
	public List<BarVO> history(@PathVariable String contractId, @PathVariable int barPeriodVal, Long from, Long to){
		if(to == null || from == null) {
			return service.getPreloadBars(contractId, BarCycleEnum.forNumber(barPeriodVal));
		}
		return service.getRangeBars(contractId, BarCycleEnum.forNumber(barPeriodVal), from, to);
	}

}
