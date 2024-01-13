package org.dromara.northstar.web.restful;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/version")
public class VersionController {

	@Autowired(required = false)
	BuildProperties buildProperties;
	
	@GetMapping
	public String getVersion() {
		if(buildProperties == null) {
			return "dev-only";
		}
		return buildProperties.getVersion();
	}
	
}
