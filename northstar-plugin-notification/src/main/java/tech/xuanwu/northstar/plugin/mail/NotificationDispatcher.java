package tech.xuanwu.northstar.plugin.mail;

import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tech.xuanwu.northstar.common.event.AbstractEventHandler;
import tech.xuanwu.northstar.common.event.GenericEventHandler;
import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.common.event.PluginEventBus;
import xyz.redtorch.pb.CoreField.NoticeField;

@Component
public class NotificationDispatcher extends AbstractEventHandler implements InitializingBean, GenericEventHandler{

	@Autowired
	private PluginEventBus peBus;
	
	@Autowired
	private List<MessageSender> senderList;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		peBus.register(this);
	}


	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return NorthstarEventType.NOTICE == eventType;
	}


	@Override
	protected void doHandle(NorthstarEvent e) {
		NoticeField notice = (NoticeField) e.getData();
		Message msg = new Message(notice);
		for(MessageSender sender : senderList) {
			sender.send(msg);
		}
	}

}
