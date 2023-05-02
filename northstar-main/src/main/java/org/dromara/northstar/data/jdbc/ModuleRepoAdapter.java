package org.dromara.northstar.data.jdbc;

import java.util.List;

import org.dromara.northstar.common.model.ModuleDealRecord;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.data.IModuleRepository;
import org.springframework.jdbc.core.JdbcTemplate;

public class ModuleRepoAdapter implements IModuleRepository{

	public ModuleRepoAdapter() {
	}

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
