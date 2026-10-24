package org.dromara.northstar.app.rl;

import java.nio.file.Files;
import java.nio.file.Path;

import org.dromara.northstar.rl.agent.RLModelLoader;
import org.dromara.northstar.rl.model.RLModelStore;
import org.dromara.northstar.rl.training.PythonTrainExecutor;
import org.dromara.northstar.rl.training.RLTrainExecutor;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.rl.RLStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * RL 训练功能的 Spring 装配。
 */
@Slf4j
@Configuration
public class RLConfig {

	/** 模型根目录：默认取工作目录下 models；仓库布局下自动回退到 northstar-rl/models */
	@Bean
	RLModelStore rlModelStore(@Value("${northstar.rl.models-root:models}") String modelsRoot,
			ApplicationContext appCtx) {
		Path dir = RLModelStore.resolveRoot(modelsRoot);
		log.info("RL 模型根目录: {}", dir.toAbsolutePath());
		RLModelStore store = new RLModelStore(dir);
		// 发布给策略侧实盘加载（RLModelLoader），保证训练/实盘/配置三方同根目录
		RLModelLoader.publishModelsRoot(dir);
		// 注册全部 RL 策略：模型级 metadata 缺失时可按策略现场 spec 现场重建（bootstrap）
		appCtx.getBeansWithAnnotation(StrategicComponent.class).values().stream()
				.filter(RLStrategy.class::isInstance)
				.map(RLStrategy.class::cast)
				.forEach(store::registerStrategy);
		return store;
	}

	/**
	 * 单轮训练执行器：默认 python 子进程；stub 用于不依赖 PyTorch 的集成测试环境。
	 * train_round.py 所在目录同样按候选路径解析。
	 * Python 解释器：显式配置（northstar.rl.python / NS_RL_PYTHON）优先；
	 * 未显式配置（裸 "python"）时自动探测环境脚本安装的 rl-venv——
	 * 训练依赖（numpy/torch/gymnasium）只装在 venv 中，直接用系统 python 会报 ModuleNotFoundError。
	 */
	@Bean
	RLTrainExecutor rlTrainExecutor(
			@Value("${northstar.rl.python:python}") String python,
			@Value("${northstar.rl.training-dir:}") String trainingDir,
			@Value("${northstar.rl.train-executor:python}") String executorType) {
		if ("stub".equalsIgnoreCase(executorType)) {
			log.warn("RL 训练执行器使用 stub 实现（不产生真实模型，仅用于测试）");
			return new StubTrainExecutor();
		}
		Path dir = firstExisting(trainingDir, "northstar-rl/training", "../northstar-rl/training", "training");
		python = resolvePython(python);
		log.info("RL 训练脚本目录: {}，Python 解释器: {}", dir.toAbsolutePath(), python);
		return new PythonTrainExecutor(python, dir);
	}

	/**
	 * 解析 Python 解释器路径：非默认配置原样使用；默认 "python" 时优先环境脚本（env.sh / env.ps1）
	 * 安装的 rl-venv 虚拟环境（与 JDK 同级的运行环境目录，生产部署无源码仓库），均无则回退系统 python。
	 */
	private static String resolvePython(String configured) {
		if (configured == null || configured.isBlank() || !"python".equals(configured.trim())) {
			return configured == null || configured.isBlank() ? "python" : configured;
		}
		String home = System.getProperty("user.home", "");
		String[] venvCandidates = {
				"c:/northstar_env/rl-venv/Scripts/python.exe",	// Windows（env.ps1 默认安装位）
				home + "/northstar-env/rl-venv/bin/python",		// Linux（env.sh 默认安装位）
				"../northstar_env/rl-venv/Scripts/python.exe",	// 工作目录为 northstar_dist 时同级回退
				"../northstar-env/rl-venv/bin/python" };
		for (String c : venvCandidates) {
			if (Files.isRegularFile(Path.of(c))) {
				return c;
			}
		}
		return "python";
	}

	/** 训练数据管理器：数据加载阶段从数据源拉取并落地 CSV，训练阶段只读 CSV */
	@Bean
	RLTrainingDataManager rlTrainingDataManager(@Value("${northstar.rl.data-root:data/rl-training}") String dataRoot) {
		Path dir = Path.of(dataRoot);
		log.info("RL 训练数据根目录: {}", dir.toAbsolutePath());
		return new RLTrainingDataManager(dir);
	}

	private static Path firstExisting(String... candidates) {
		for (String c : candidates) {
			if (c != null && !c.isBlank() && Files.isDirectory(Path.of(c))) {
				return Path.of(c);
			}
		}
		// 都不存在时返回首个配置值（由调用方决定创建或报错）
		return Path.of(candidates[0] == null || candidates[0].isBlank() ? "models" : candidates[0]);
	}
}
