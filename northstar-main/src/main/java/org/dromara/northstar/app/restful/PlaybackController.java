package org.dromara.northstar.app.restful;

import java.util.List;

import org.dromara.northstar.app.service.PlaybackService;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.BarVO;
import org.dromara.northstar.common.model.PlaybackStatusVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
	
	@GetMapping("next/{interval}")
	public void playNext(@PathVariable String interval) {
		service.playNext(Interval.fromAlias(interval));
	}
	
	@GetMapping("status")
	public PlaybackStatusVO status() {
		return service.getPlaybackStatus();
	}
	
	@GetMapping("history/{contractId}/{interval}")
	public List<BarVO> history(@PathVariable String contractId, @PathVariable String interval, Long from, Long to){
		if(to == null || from == null) {
			return service.getPreloadBars(contractId, Interval.fromAlias(interval));
		}
		return service.getRangeBars(contractId, Interval.fromAlias(interval), from, to);
	}

}
