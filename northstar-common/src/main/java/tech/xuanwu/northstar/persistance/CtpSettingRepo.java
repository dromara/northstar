package tech.xuanwu.northstar.persistance;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import tech.xuanwu.northstar.persistance.po.CtpSetting;
import tech.xuanwu.northstar.persistance.po.CtpSetting.MarketType;


@Repository
public interface CtpSettingRepo extends CrudRepository<CtpSetting, String> {

	List<CtpSetting> findByMarketType(MarketType marketType);
}
