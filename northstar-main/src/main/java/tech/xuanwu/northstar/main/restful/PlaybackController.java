package tech.xuanwu.northstar.main.restful;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.xuanwu.northstar.common.model.PlaybackDescription;
import tech.xuanwu.northstar.common.model.ResultBean;
import tech.xuanwu.northstar.main.manager.ModuleManager;
import tech.xuanwu.northstar.main.service.PlaybackService;

@RestController
@RequestMapping("/pb")
public class PlaybackController {
	
	@Autowired
	private PlaybackService playbackService;
	
	@Autowired 
	private ModuleManager moduleMgr;

	/**
	 * 开始回测
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws Exception 
	 */
	@PostMapping("/play")
	public ResultBean<Void> play(@RequestBody PlaybackDescription playbackDescription) throws Exception{
		playbackService.play(playbackDescription, moduleMgr);
		return new ResultBean<>(null);
	}
	
	/**
	 * 查询回测进度
	 * @param playId
	 * @return
	 */
	@GetMapping("/play/process")
	public ResultBean<Integer> playProcess(){
		return new ResultBean<>(playbackService.playProcess());
	}
	
	/**
	 * 查询回测账户余额
	 * @param moduleName
	 * @return
	 */
	@GetMapping("/balance")
	public ResultBean<Integer> playbackBalance(@NotNull String moduleName){
		return new ResultBean<>(playbackService.playbackBalance(moduleName));
	}
	
//	/**
//	 * 查询回测记录
//	 * @return
//	 */
//	@GetMapping("/record")
//	public ResultBean<PlaybackRecord> playbackRecord(@NotNull String moduleName){
//		return new Result;
//	}
	
	/**
	 * 查询回测就绪状态
	 * @return
	 */
	@GetMapping("/readiness")
	public ResultBean<Boolean> getPlaybackReadiness(){
		return new ResultBean<>(playbackService.getPlaybackReadiness());
	}
}
