package org.dromara.northstar.data.jdbc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.dromara.northstar.common.model.ModuleDealRecord;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.data.jdbc.entity.ModuleDealRecordDO;
import org.dromara.northstar.data.jdbc.entity.ModuleDescriptionDO;
import org.dromara.northstar.data.jdbc.entity.ModuleRuntimeDescriptionDO;

public class ModuleRepoAdapter implements IModuleRepository{
	
	private ModuleDealRecordRepository mdrDelegate;
	
	private ModuleDescriptionRepository mdDelegate;
	
	private ModuleRuntimeDescriptionRepository mrdDelegate;

	public ModuleRepoAdapter(ModuleDescriptionRepository mdDelegate, ModuleRuntimeDescriptionRepository mrdDelegate,
			ModuleDealRecordRepository mdrDelegate) {
		this.mdDelegate = mdDelegate;
		this.mrdDelegate = mrdDelegate;
		this.mdrDelegate = mdrDelegate;
	}

	@Override
	public void saveSettings(ModuleDescription moduleDescription) {
		mdDelegate.save(ModuleDescriptionDO.convertFrom(moduleDescription));
	}

	@Override
	public ModuleDescription findSettingsByName(String moduleName) {
		ModuleDescriptionDO obj = mdDelegate.findById(moduleName).orElseThrow(() -> new NoSuchElementException("找不到模组配置：" + moduleName));
		return obj.convertTo();
	}

	@Override
	public List<ModuleDescription> findAllSettings() {
		Iterator<ModuleDescriptionDO> itResults = mdDelegate.findAll().iterator();
		List<ModuleDescription> list = new ArrayList<>();
		while(itResults.hasNext()) {
			list.add(itResults.next().convertTo());
		}
		return list;
	}

	@Override
	public void deleteSettingsByName(String moduleName) {
		mdDelegate.deleteById(moduleName);
	}

	@Override
	public void saveRuntime(ModuleRuntimeDescription moduleRtDescription) {
		mrdDelegate.save(ModuleRuntimeDescriptionDO.convertFrom(moduleRtDescription));
	}

	@Override
	public ModuleRuntimeDescription findRuntimeByName(String moduleName) {
		ModuleRuntimeDescriptionDO obj = mrdDelegate.findById(moduleName).orElseThrow(() -> new NoSuchElementException("找不到模组运行时：" + moduleName));
		return obj.convertTo();
	}

	@Override
	public void deleteRuntimeByName(String moduleName) {
		mrdDelegate.deleteById(moduleName);
	}

	@Override
	public void saveDealRecord(ModuleDealRecord dealRecord) {
		mdrDelegate.save(ModuleDealRecordDO.convertFrom(dealRecord));
	}

	@Override
	public List<ModuleDealRecord> findAllDealRecords(String moduleName) {
		return mdrDelegate.findByModuleName(moduleName).stream()
				.map(ModuleDealRecordDO::convertTo)
				.toList();
	}

	@Override
	public void removeAllDealRecords(String moduleName) {
		mdrDelegate.deleteByModuleName(moduleName);
	}
}
