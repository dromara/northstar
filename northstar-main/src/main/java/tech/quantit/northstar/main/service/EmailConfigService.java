package tech.quantit.northstar.main.service;

import tech.quantit.northstar.common.model.MailConfigDescription;
import tech.quantit.northstar.data.IMailConfigRepository;
import tech.quantit.northstar.main.mail.MailDeliveryManager;

public class EmailConfigService {

	private MailDeliveryManager mailMgr;
	
	private IMailConfigRepository repo;
	
	public EmailConfigService(MailDeliveryManager mailMgr, IMailConfigRepository repo) {
		this.mailMgr = mailMgr;
		this.repo = repo;
		mailMgr.setEmailConfig(getConfig());
	}
	
	public void saveConfig(MailConfigDescription emailConfig) {
		repo.save(emailConfig);
		mailMgr.setEmailConfig(emailConfig);
	}
	
	public MailConfigDescription getConfig() {
		return repo.get();
	}
}
