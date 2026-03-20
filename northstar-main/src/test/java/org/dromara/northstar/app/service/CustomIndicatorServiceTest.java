package org.dromara.northstar.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.dromara.northstar.NorthstarApplication;
import org.dromara.northstar.common.model.CustomIndicatorVO;
import org.dromara.northstar.data.jdbc.CustomIndicatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;

@SpringBootTest(classes = NorthstarApplication.class, value = "spring.profiles.active=unittest")
class CustomIndicatorServiceTest {

	@Autowired
	private CustomIndicatorService service;

	@Autowired
	private CustomIndicatorRepository repository;

	@BeforeEach
	void cleanUp() {
		repository.deleteAll();
	}

	private CustomIndicatorVO createTestIndicator(String shortName, String series) {
		return CustomIndicatorVO.builder()
				.shortName(shortName)
				.description("测试指标描述")
				.series(series)
				.indicatorTemplateCode("function IndicatorTemplate() { return { calc: function(dataList) { return []; } }; }")
				.build();
	}

	@Test
	void testCreateIndicator() {
		CustomIndicatorVO vo = createTestIndicator("TESTMA", "price");
		CustomIndicatorVO created = service.create(vo);

		assertThat(created.getId()).isNotNull();
		assertThat(created.getShortName()).isEqualTo("TESTMA");
		assertThat(created.getSeries()).isEqualTo("price");
		assertThat(created.getFavorite()).isFalse();
		assertThat(created.getCreatedAt()).isNotNull();
		assertThat(created.getUpdatedAt()).isNotNull();
	}

	@Test
	void testCreateDuplicateIndicator() {
		CustomIndicatorVO vo = createTestIndicator("TESTMA", "price");
		service.create(vo);

		CustomIndicatorVO duplicate = createTestIndicator("TESTMA", "volume");
		assertThrows(EntityExistsException.class, () -> service.create(duplicate));
	}

	@Test
	void testFindAll() {
		service.create(createTestIndicator("IND1", "price"));
		service.create(createTestIndicator("IND2", "volume"));

		List<CustomIndicatorVO> result = service.findAll();

		assertThat(result).hasSize(2);
	}

	@Test
	void testFindBySeries() {
		service.create(createTestIndicator("IND1", "price"));
		service.create(createTestIndicator("IND2", "volume"));
		service.create(createTestIndicator("IND3", "price"));

		List<CustomIndicatorVO> priceIndicators = service.findBySeries("price");

		assertThat(priceIndicators).hasSize(2);
		assertThat(priceIndicators).allMatch(ind -> "price".equals(ind.getSeries()));
	}

	@Test
	void testFindById() {
		CustomIndicatorVO created = service.create(createTestIndicator("TESTMA", "price"));

		CustomIndicatorVO found = service.findById(created.getId());

		assertThat(found.getId()).isEqualTo(created.getId());
		assertThat(found.getShortName()).isEqualTo("TESTMA");
	}

	@Test
	void testFindByIdNotFound() {
		assertThrows(EntityNotFoundException.class, () -> service.findById("non-existent-id"));
	}

	@Test
	void testUpdateIndicator() {
		CustomIndicatorVO created = service.create(createTestIndicator("TESTMA", "price"));

		CustomIndicatorVO update = CustomIndicatorVO.builder()
				.shortName("TESTMA")
				.description("更新后的描述")
				.series("volume")
				.indicatorTemplateCode("function IndicatorTemplate() { return { calc: function(dataList) { return []; } }; }")
				.build();

		CustomIndicatorVO updated = service.update(created.getId(), update);

		assertThat(updated.getDescription()).isEqualTo("更新后的描述");
		assertThat(updated.getSeries()).isEqualTo("volume");
	}

	@Test
	void testUpdateIndicatorNotFound() {
		CustomIndicatorVO update = createTestIndicator("TESTMA", "price");

		assertThrows(EntityNotFoundException.class, () -> service.update("non-existent-id", update));
	}

	@Test
	void testUpdateIndicatorWithDuplicateName() {
		CustomIndicatorVO ind1 = service.create(createTestIndicator("IND1", "price"));
		service.create(createTestIndicator("IND2", "price"));

		CustomIndicatorVO update = CustomIndicatorVO.builder()
				.shortName("IND2")
				.description("描述")
				.series("price")
				.indicatorTemplateCode("function IndicatorTemplate() { return { calc: function(dataList) { return []; } }; }")
				.build();

		assertThrows(EntityExistsException.class, () -> service.update(ind1.getId(), update));
	}

	@Test
	void testDeleteIndicator() {
		CustomIndicatorVO created = service.create(createTestIndicator("TESTMA", "price"));
		assertThat(service.findAll()).hasSize(1);

		service.delete(created.getId());

		assertThat(service.findAll()).isEmpty();
	}

	@Test
	void testDeleteIndicatorNotFound() {
		assertThrows(EntityNotFoundException.class, () -> service.delete("non-existent-id"));
	}

	@Test
	void testSetFavorite() {
		CustomIndicatorVO created = service.create(createTestIndicator("TESTMA", "price"));
		assertThat(created.getFavorite()).isFalse();

		CustomIndicatorVO favorited = service.setFavorite(created.getId(), true);

		assertThat(favorited.getFavorite()).isTrue();
	}

	@Test
	void testCancelFavorite() {
		CustomIndicatorVO created = service.create(createTestIndicator("TESTMA", "price"));
		service.setFavorite(created.getId(), true);

		CustomIndicatorVO unfavorited = service.setFavorite(created.getId(), false);

		assertThat(unfavorited.getFavorite()).isFalse();
	}

	@Test
	void testSetFavoriteNotFound() {
		assertThrows(EntityNotFoundException.class, () -> service.setFavorite("non-existent-id", true));
	}

	@Test
	void testUpdateUpdatesTimestamp() throws InterruptedException {
		CustomIndicatorVO created = service.create(createTestIndicator("TESTMA", "price"));
		Long originalUpdatedAt = created.getUpdatedAt();

		Thread.sleep(10); // 确保时间戳有变化

		CustomIndicatorVO update = CustomIndicatorVO.builder()
				.shortName("TESTMA")
				.description("更新描述")
				.series("price")
				.indicatorTemplateCode("function IndicatorTemplate() { return { calc: function(dataList) { return []; } }; }")
				.build();
		CustomIndicatorVO updated = service.update(created.getId(), update);

		assertThat(updated.getUpdatedAt()).isGreaterThan(originalUpdatedAt);
	}
}
