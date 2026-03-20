package org.dromara.northstar.app.restful;

import org.dromara.northstar.app.service.NotificationService;
import org.dromara.northstar.common.model.NotificationSettingsDescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("northstar/notification")
@RestController
public class NotificationController {
	
	@Autowired
	private NotificationService service;

	@GetMapping("settings")
	public NotificationSettingsDescription loadSettings(){
		return service.loadSettings();
	}
	
	@PostMapping("settings")
	public void saveSettings(@RequestBody NotificationSettingsDescription settings){
		service.saveSettings(settings);
	}
	
	@DeleteMapping("settings/reset")
	public NotificationSettingsDescription resetSettings(){
		return service.resetSettings();
	}
	
	@PostMapping("settings/test")
	public void testSettings(@RequestBody NotificationSettingsDescription settings){
		service.testSettings(settings);
	}
}
