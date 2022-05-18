package tech.quantit.northstar.main.restful;

import java.net.SocketException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.quantit.northstar.main.utils.InetAddressUtils;

@RestController
public class RedirectController {

	@GetMapping("/redirect")
	public String redirectUrl() throws SocketException {
		return InetAddressUtils.getInet4Address();
	}
}
