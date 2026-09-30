package org.dromara.northstar.rl.agent;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * ModelMetadata 加载示例：以 src/test/resources 下的 cartpole metadata 为例。
 * 纯 JSON 解析，无需 PyTorch 原生库。
 */
class ModelMetadataTest {

    private static final Path MODEL_DIR = Path.of("src/test/resources/models/cartpole");

    @Test
    void loadsCartpoleMetadata() throws Exception {
        ModelMetadata meta = ModelMetadata.load(MODEL_DIR);
        assertEquals("cartpole-dqn", meta.getName());
        assertEquals(4, meta.getStateDim());
        assertEquals(2, meta.getActionDim());
        assertArrayEquals(new String[] {"LEFT", "RIGHT"}, meta.getActions());
        assertEquals("model.pt", meta.getModelFile());
        assertEquals("replay.csv", meta.getDataFile());
        assertEquals("identity", meta.getPreprocessing().getType());
        assertEquals(128, meta.getHidden());
    }
}
