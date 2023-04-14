package org.dromara.northstar.data.redis;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import com.alibaba.fastjson2.JSON;

import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.common.model.ModuleDealRecord;
import tech.quantit.northstar.common.model.ModuleDescription;
import tech.quantit.northstar.common.model.ModuleRuntimeDescription;
import tech.quantit.northstar.data.IModuleRepository;

/**
 * 
 * @author KevinHuangwl
 *
 */
public class ModuleRepoRedisImpl implements IModuleRepository{

	private RedisTemplate<String, byte[]> redisTemplate;

	private static final String KEY_PREFIX = Constants.APP_NAME + "Module:";
	
	private static final String KEY_SETTING = "Settings:";
	
	private static final String KEY_RUNTIME = "Runtime:";
	
	private static final String KEY_REC = "DealRecord:";
	
	public ModuleRepoRedisImpl(RedisTemplate<String, byte[]> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}
	
	@Override
	public void saveSettings(ModuleDescription moduleDescription) {
		String key = getKey(moduleDescription.getModuleName(), KEY_SETTING);
		redisTemplate.boundValueOps(key).set(JSON.toJSONBytes(moduleDescription));
	}

	@Override
	public ModuleDescription findSettingsByName(String moduleName) {
		String key = getKey(moduleName, KEY_SETTING);
		byte[] data = redisTemplate.boundValueOps(key).get();
		if(data == null)	return null;
		return JSON.parseObject(data, ModuleDescription.class);
	}
	
	private String getKey(String moduleName, String type) {
		return KEY_PREFIX + type + moduleName;
	}

	@Override
	public List<ModuleDescription> findAllSettings() {
		Set<String> keys = redisTemplate.keys(KEY_PREFIX + KEY_SETTING + "*");
		return keys.stream()
				.map(key -> redisTemplate.boundValueOps(key).get())
				.map(data -> JSON.parseObject(data, ModuleDescription.class))
				.toList();
	}

	@Override
	public void deleteSettingsByName(String moduleName) {
		String key = getKey(moduleName, KEY_SETTING);
		redisTemplate.delete(key);
	}

	@Override
	public void saveRuntime(ModuleRuntimeDescription moduleRtDescription) {
		String key = getKey(moduleRtDescription.getModuleName(), KEY_RUNTIME);
		redisTemplate.boundValueOps(key).set(JSON.toJSONBytes(moduleRtDescription));
	}

	@Override
	public ModuleRuntimeDescription findRuntimeByName(String moduleName) {
		String key = getKey(moduleName, KEY_RUNTIME);
		byte[] data = redisTemplate.boundValueOps(key).get();
		if(data == null)	return null;
		return JSON.parseObject(data, ModuleRuntimeDescription.class);
	}

	@Override
	public void deleteRuntimeByName(String moduleName) {
		String key = getKey(moduleName, KEY_RUNTIME);
		redisTemplate.delete(key);
	}

	@Override
	public void saveDealRecord(ModuleDealRecord dealRecord) {
		String key = getKey(dealRecord.getModuleName(), KEY_REC);
		redisTemplate.boundListOps(key).rightPush(JSON.toJSONBytes(dealRecord));
		
	}

	@Override
	public List<ModuleDealRecord> findAllDealRecords(String moduleName) {
		String key = getKey(moduleName, KEY_REC);
		BoundListOperations<String, byte[]> list = redisTemplate.boundListOps(key);
		return Optional.ofNullable(list.range(0, list.size()))
				.orElse(Collections.emptyList())
				.stream()
				.map(data -> JSON.parseObject(data, ModuleDealRecord.class))
				.toList();
	}

	@Override
	public void removeAllDealRecords(String moduleName) {
		String key = getKey(moduleName, KEY_REC);
		redisTemplate.delete(key);
	}

}
