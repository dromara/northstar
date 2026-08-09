package org.dromara.northstar.app.restful;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.dromara.northstar.NorthstarApplication;
import org.dromara.northstar.app.service.MultichartsService;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.OverlayVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.alibaba.fastjson2.JSON;

@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=unittest")
@AutoConfigureMockMvc
class MultichartsControllerTest {

	@Autowired
	MockMvc mvc;
	
	@MockitoBean
	MultichartsService service;
	
	OverlayVO overlay = OverlayVO.builder()
			.id("test-overlay-id")
			.name("Test Overlay")
			.build();
	
	@BeforeEach
	void setUp() {
		when(service.getOverlays("BTCUSDT@BINANCE@SPOT@PLAYBACK", Interval.M10)).thenReturn(List.of(overlay));
	}
	
	@Test
	void testSaveOverlay() throws Exception {
		mvc.perform(MockMvcRequestBuilders.post("/northstar/multicharts/overlay/BTCUSDT@BINANCE@SPOT@PLAYBACK")
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSON.toJSONString(overlay)))
			.andExpect(status().isOk());
	}

	@Test
	void testGetOverlays() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/northstar/multicharts/overlay/BTCUSDT@BINANCE@SPOT@PLAYBACK/10m"))
			.andExpect(status().isOk());
	}

	@Test
	void testDeleteOverlay() throws Exception {
		mvc.perform(MockMvcRequestBuilders.delete("/northstar/multicharts/overlay/test001"))
			.andExpect(status().isOk());
	}

	@Test
	void testDeleteAllOverlay() throws Exception {
		mvc.perform(MockMvcRequestBuilders.delete("/northstar/multicharts/overlays/test001"))
			.andExpect(status().isOk());
	}

}
