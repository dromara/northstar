package tech.quantit.northstar.main.restful;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StaticController {

	@GetMapping("/")
	public String index() {
		return "index.html";
	}
}
