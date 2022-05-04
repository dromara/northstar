package tech.quantit.northstar.data.redis;

import java.util.List;

import tech.quantit.northstar.common.model.ModuleDealRecord;
import tech.quantit.northstar.common.model.ModuleDescription;
import tech.quantit.northstar.common.model.ModuleSettingsDescription;
import tech.quantit.northstar.data.IModuleRepository;

public class ModuleRepoRedisImpl implements IModuleRepository{

	@Override
	public void saveSettings(ModuleSettingsDescription moduleSettingsDescription) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ModuleSettingsDescription findSettingsByName(String moduleName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ModuleSettingsDescription> findAll() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteSettingsByName(String moduleName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void save(ModuleDescription moduleDescription) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ModuleDescription findByName(String moduleName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteByName(String moduleName) {
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
