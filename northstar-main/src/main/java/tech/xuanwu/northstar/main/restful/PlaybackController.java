package tech.xuanwu.northstar.main.restful;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.xuanwu.northstar.common.constant.PlaybackPrecision;
import tech.xuanwu.northstar.common.model.PlaybackRecord;
import tech.xuanwu.northstar.common.model.ResultBean;

@RestController
@RequestMapping("/pb")
public class PlaybackController {

	/**
	 * 开始回测
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	@PostMapping("/play")
	public ResultBean<List<String>> play(@NotNull String startDate, @NotNull String endDate, @NotNull PlaybackPrecision precision,
			@RequestBody @NotNull List<String> moduleNames){
		return null;
	}
	
	/**
	 * 查询回测进度
	 * @param playId
	 * @return
	 */
	@GetMapping("/play/process")
	public ResultBean<Integer> playProcess(@NotNull String playId){
		return null;
	}
	
	/**
	 * 查询回测记录
	 * @return
	 */
	@GetMapping("/record")
	public ResultBean<PlaybackRecord> playbackRecord(@NotNull String moduleName){
		return null;
	}
	
	/**
	 * 查询回测就绪状态
	 * @return
	 */
	@GetMapping("/readiness")
	public ResultBean<Boolean> getPlaybackReadiness(){
		return null;
	}
}
