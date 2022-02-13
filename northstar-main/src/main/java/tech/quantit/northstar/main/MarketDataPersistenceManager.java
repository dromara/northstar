package tech.quantit.northstar.main;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tech.quantit.northstar.main.config.MarketDataPersistenceConfiguration;

@Component
public class MarketDataPersistenceManager {

	@Autowired
	private MarketDataPersistenceConfiguration persistConfig;
	
	private Set<Pattern> ptnSet = new HashSet<>();
	
	@PostConstruct
	private void init() {
		for(String str : persistConfig.getAllowPersistence()) {
			ptnSet.add(Pattern.compile(str, Pattern.CASE_INSENSITIVE));
		}
	}
	
	public boolean isAllowedPersistence(String unifiedSymbol) {
		for(Pattern ptn : ptnSet) {
			if(ptn.matcher(unifiedSymbol).find()) {
				return true;
			}
		}
		return false;
	}
	
}
