package tech.quantit.northstar.data.redis;

import java.util.List;

import tech.quantit.northstar.common.model.ModuleDealRecord;
import tech.quantit.northstar.common.model.ModuleRuntimeDescription;
import tech.quantit.northstar.common.model.ModuleDescription;
import tech.quantit.northstar.data.IModuleRepository;

public class ModuleRepoRedisImpl implements IModuleRepository{

	@Override
	public void saveSettings(ModuleDescription moduleSettingsDescription) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ModuleDescription findSettingsByName(String moduleName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ModuleDescription> findAllSettings() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteSettingsByName(String moduleName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveRuntime(ModuleRuntimeDescription moduleDescription) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ModuleRuntimeDescription findRuntimeByName(String moduleName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteRuntimeByName(String moduleName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveDealRecord(ModuleDealRecord dealRecord) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<ModuleDealRecord> findAllDealRecords(String moduleName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeAllDealRecords(String moduleName) {
		// TODO Auto-generated method stub
		
	}

}
