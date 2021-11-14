package tech.xuanwu.northstar.main.service;

import java.util.List;

import tech.xuanwu.northstar.common.model.PlaybackDescription;
import tech.xuanwu.northstar.common.model.PlaybackRecord;
import tech.xuanwu.northstar.main.persistence.MarketDataRepository;
import tech.xuanwu.northstar.main.playback.PlaybackTask;

public class PlaybackService {
	
	private PlaybackTask task;
	
	private MarketDataRepository mdRepo;
	

	public List<String> play(PlaybackDescription playbackDescription){
		return null;
	}
	
	public Integer playProcess(String playId){
		return null;
	}
	
	public PlaybackRecord playbackRecord(String moduleName){
		return null;
	}
	
	public boolean getPlaybackReadiness(){
		return task == null || task.isDone();
	}
}
