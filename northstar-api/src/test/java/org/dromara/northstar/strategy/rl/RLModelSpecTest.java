package org.dromara.northstar.strategy.rl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class RLModelSpecTest {

	RLModelSpec spec() {
		return new RLModelSpec(List.of(
				new RLFeatureSpec("ret1", NormalizerType.RATIO),
				new RLFeatureSpec("maDev", NormalizerType.RATIO),
				new RLFeatureSpec("position", NormalizerType.NONE)),
				TradeAction.class, PreprocessType.IDENTITY);
	}

	@Test
	void dimsAndActions() {
		RLModelSpec spec = spec();
		assertThat(spec.stateDim()).isEqualTo(3);
		assertThat(spec.actionDim()).isEqualTo(3);
		assertThat(spec.actions()).containsExactly("HOLD", "BUY", "SELL");
	}

	@Test
	void specHashIsStable() {
		assertThat(spec().specHash()).isEqualTo(spec().specHash()).hasSize(16);
	}

	@Test
	void specHashSensitiveToFeatureOrder() {
		RLModelSpec reordered = new RLModelSpec(List.of(
				new RLFeatureSpec("maDev", NormalizerType.RATIO),
				new RLFeatureSpec("ret1", NormalizerType.RATIO),
				new RLFeatureSpec("position", NormalizerType.NONE)),
				TradeAction.class, PreprocessType.IDENTITY);
		assertThat(reordered.specHash()).isNotEqualTo(spec().specHash());
	}

	@Test
	void specHashSensitiveToNormalizerAndPreprocessing() {
		RLModelSpec otherNormalizer = new RLModelSpec(List.of(
				new RLFeatureSpec("ret1", NormalizerType.Z_SCORE),
				new RLFeatureSpec("maDev", NormalizerType.RATIO),
				new RLFeatureSpec("position", NormalizerType.NONE)),
				TradeAction.class, PreprocessType.IDENTITY);
		RLModelSpec otherPreprocess = new RLModelSpec(List.of(
				new RLFeatureSpec("ret1", NormalizerType.RATIO),
				new RLFeatureSpec("maDev", NormalizerType.RATIO),
				new RLFeatureSpec("position", NormalizerType.NONE)),
				TradeAction.class, PreprocessType.STANDARDIZE);
		assertThat(otherNormalizer.specHash()).isNotEqualTo(spec().specHash());
		assertThat(otherPreprocess.specHash()).isNotEqualTo(spec().specHash());
	}

	@Test
	void diffReportsStructuralChanges() {
		RLModelSpec changed = new RLModelSpec(List.of(
				new RLFeatureSpec("ret1", NormalizerType.Z_SCORE),	// 变更
				new RLFeatureSpec("maDev", NormalizerType.RATIO),
				new RLFeatureSpec("position", NormalizerType.NONE),
				new RLFeatureSpec("atrRatio", NormalizerType.RATIO)),	// 新增
				TradeAction.class, PreprocessType.STANDARDIZE);		// 预处理变更
		List<String> diffs = spec().diff(changed);
		assertThat(diffs).anySatisfy(d -> assertThat(d).contains("特征[0]变更").contains("Z_SCORE"));
		assertThat(diffs).anySatisfy(d -> assertThat(d).contains("特征[3]新增").contains("atrRatio"));
		assertThat(diffs).anySatisfy(d -> assertThat(d).contains("预处理变更"));
		assertThat(spec().diff(spec())).isEmpty();
	}

	@Test
	void rejectInvalidSpec() {
		assertThatThrownBy(() -> new RLModelSpec(List.of(), TradeAction.class, PreprocessType.IDENTITY))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new RLModelSpec(List.of(new RLFeatureSpec("a", NormalizerType.NONE)), null, PreprocessType.IDENTITY))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
