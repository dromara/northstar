package org.dromara.northstar.app.restful;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;

import org.dromara.northstar.NorthstarApplication;
import org.dromara.northstar.app.service.CustomIndicatorService;
import org.dromara.northstar.common.model.CustomIndicatorVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.alibaba.fastjson2.JSON;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;

@SpringBootTest(classes = NorthstarApplication.class, value = "spring.profiles.active=unittest")
@AutoConfigureMockMvc
class CustomIndicatorControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private CustomIndicatorService service;

	private CustomIndicatorVO createTestIndicator(String id, String shortName, String series) {
		return CustomIndicatorVO.builder()
				.id(id)
				.shortName(shortName)
				.description("测试指标")
				.series(series)
				.indicatorTemplateCode("function IndicatorTemplate() { return { calc: function(dataList) { return []; } }; }")
				.favorite(false)
				.createdAt(System.currentTimeMillis())
				.updatedAt(System.currentTimeMillis())
				.build();
	}

	@Test
	void testGetAll() throws Exception {
		List<CustomIndicatorVO> indicators = Arrays.asList(
				createTestIndicator("1", "MA5", "price"),
				createTestIndicator("2", "VOL", "volume"));
		when(service.findAll()).thenReturn(indicators);

		mvc.perform(MockMvcRequestBuilders.get("/northstar/custom-indicators"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].shortName").value("MA5"))
				.andExpect(jsonPath("$[1].shortName").value("VOL"));
	}

	@Test
	void testGetAllWithSeriesFilter() throws Exception {
		List<CustomIndicatorVO> indicators = Arrays.asList(createTestIndicator("1", "MA5", "price"));
		when(service.findBySeries("price")).thenReturn(indicators);

		mvc.perform(MockMvcRequestBuilders.get("/northstar/custom-indicators?series=price"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].series").value("price"));
	}

	@Test
	void testGetById() throws Exception {
		CustomIndicatorVO indicator = createTestIndicator("test-id", "MA5", "price");
		when(service.findById("test-id")).thenReturn(indicator);

		mvc.perform(MockMvcRequestBuilders.get("/northstar/custom-indicators/test-id"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value("test-id"))
				.andExpect(jsonPath("$.shortName").value("MA5"));
	}

	@Test
	void testGetByIdNotFound() throws Exception {
		when(service.findById("not-found"))
				.thenThrow(new EntityNotFoundException("指标不存在: not-found"));

		mvc.perform(MockMvcRequestBuilders.get("/northstar/custom-indicators/not-found"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("指标不存在: not-found"));
	}

	@Test
	void testCreate() throws Exception {
		CustomIndicatorVO request = CustomIndicatorVO.builder()
				.shortName("MA5")
				.description("测试MA")
				.series("price")
				.indicatorTemplateCode("function IndicatorTemplate() { return {}; }")
				.build();

		CustomIndicatorVO created = createTestIndicator("new-id", "MA5", "price");
		when(service.create(any(CustomIndicatorVO.class))).thenReturn(created);

		mvc.perform(MockMvcRequestBuilders.post("/northstar/custom-indicators")
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSON.toJSONString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value("new-id"))
				.andExpect(jsonPath("$.shortName").value("MA5"));
	}

	@Test
	void testCreateConflict() throws Exception {
		CustomIndicatorVO request = CustomIndicatorVO.builder()
				.shortName("MA5")
				.description("测试MA")
				.series("price")
				.indicatorTemplateCode("function IndicatorTemplate() { return {}; }")
				.build();

		when(service.create(any(CustomIndicatorVO.class)))
				.thenThrow(new EntityExistsException("指标简称已存在: MA5"));

		mvc.perform(MockMvcRequestBuilders.post("/northstar/custom-indicators")
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSON.toJSONString(request)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("指标简称已存在: MA5"));
	}

	@Test
	void testUpdate() throws Exception {
		CustomIndicatorVO request = CustomIndicatorVO.builder()
				.shortName("MA10")
				.description("更新后的描述")
				.series("volume")
				.indicatorTemplateCode("function IndicatorTemplate() { return {}; }")
				.build();

		CustomIndicatorVO updated = createTestIndicator("test-id", "MA10", "volume");
		updated.setDescription("更新后的描述");

		when(service.update(eq("test-id"), any(CustomIndicatorVO.class))).thenReturn(updated);

		mvc.perform(MockMvcRequestBuilders.put("/northstar/custom-indicators/test-id")
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSON.toJSONString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.shortName").value("MA10"))
				.andExpect(jsonPath("$.series").value("volume"));
	}

	@Test
	void testUpdateNotFound() throws Exception {
		CustomIndicatorVO request = CustomIndicatorVO.builder()
				.shortName("MA10")
				.description("描述")
				.series("price")
				.indicatorTemplateCode("function IndicatorTemplate() { return {}; }")
				.build();

		when(service.update(eq("not-found"), any(CustomIndicatorVO.class)))
				.thenThrow(new EntityNotFoundException("指标不存在: not-found"));

		mvc.perform(MockMvcRequestBuilders.put("/northstar/custom-indicators/not-found")
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSON.toJSONString(request)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("指标不存在: not-found"));
	}

	@Test
	void testUpdateConflict() throws Exception {
		CustomIndicatorVO request = CustomIndicatorVO.builder()
				.shortName("EXISTING")
				.description("描述")
				.series("price")
				.indicatorTemplateCode("function IndicatorTemplate() { return {}; }")
				.build();

		when(service.update(eq("test-id"), any(CustomIndicatorVO.class)))
				.thenThrow(new EntityExistsException("指标简称已存在: EXISTING"));

		mvc.perform(MockMvcRequestBuilders.put("/northstar/custom-indicators/test-id")
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSON.toJSONString(request)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("指标简称已存在: EXISTING"));
	}

	@Test
	void testDelete() throws Exception {
		mvc.perform(MockMvcRequestBuilders.delete("/northstar/custom-indicators/test-id"))
				.andExpect(status().isNoContent());
	}

	@Test
	void testDeleteNotFound() throws Exception {
		doThrow(new EntityNotFoundException("指标不存在: not-found"))
				.when(service).delete("not-found");

		mvc.perform(MockMvcRequestBuilders.delete("/northstar/custom-indicators/not-found"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("指标不存在: not-found"));
	}

	@Test
	void testSetFavorite() throws Exception {
		CustomIndicatorVO updated = createTestIndicator("test-id", "MA5", "price");
		updated.setFavorite(true);

		when(service.setFavorite("test-id", true)).thenReturn(updated);

		mvc.perform(MockMvcRequestBuilders.put("/northstar/custom-indicators/test-id/favorite")
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSON.toJSONString(new java.util.HashMap<String, Boolean>() {{ put("favorite", true); }})))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.favorite").value(true));
	}

	@Test
	void testCancelFavorite() throws Exception {
		CustomIndicatorVO updated = createTestIndicator("test-id", "MA5", "price");
		updated.setFavorite(false);

		when(service.setFavorite("test-id", false)).thenReturn(updated);

		mvc.perform(MockMvcRequestBuilders.put("/northstar/custom-indicators/test-id/favorite")
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSON.toJSONString(new java.util.HashMap<String, Boolean>() {{ put("favorite", false); }})))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.favorite").value(false));
	}

	@Test
	void testSetFavoriteNotFound() throws Exception {
		when(service.setFavorite("not-found", true))
				.thenThrow(new EntityNotFoundException("指标不存在: not-found"));

		mvc.perform(MockMvcRequestBuilders.put("/northstar/custom-indicators/not-found/favorite")
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSON.toJSONString(new java.util.HashMap<String, Boolean>() {{ put("favorite", true); }})))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("指标不存在: not-found"));
	}

	@Test
	void testSetFavoriteMissingParam() throws Exception {
		mvc.perform(MockMvcRequestBuilders.put("/northstar/custom-indicators/test-id/favorite")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest());
	}
}
