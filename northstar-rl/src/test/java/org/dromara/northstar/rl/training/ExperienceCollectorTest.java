package org.dromara.northstar.rl.training;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.dromara.northstar.rl.agent.ModelMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * ExperienceCollector 采样示例的保守验证：模型文件不存在时走纯随机策略冷启动，
 * 不加载任何模型，无需 PyTorch 原生库。
 */
class ExperienceCollectorTest {

    @TempDir
    Path modelDir;

    private ModelMetadata meta;

    @BeforeEach
    void setUp() throws Exception {
        Files.writeString(modelDir.resolve(ModelMetadata.FILE_NAME), """
                {"name":"cartpole-dqn","version":1,"state_dim":4,"action_dim":2,
                 "actions":["LEFT","RIGHT"],"model_file":"model.pt","data_file":"replay.csv"}
                """);
        meta = ModelMetadata.load(modelDir);
    }

    @Test
    void collectWithRandomPolicyWritesTransitions() throws Exception {
        Path out = modelDir.resolve(meta.getDataFile());
        int episodes = 3;
        int steps = ExperienceCollector.collect(meta, modelDir, 1.0, episodes, out, 42);

        assertTrue(steps > 0);
        List<String> lines = Files.readAllLines(out);
        assertEquals("s0,s1,s2,s3,action,reward,ns0,ns1,ns2,ns3,terminated", lines.get(0));
        assertEquals(steps, lines.size() - 1, "每步应产生一条转移样本");
        for (String line : lines.subList(1, lines.size())) {
            String[] cols = line.split(",");
            assertEquals(11, cols.length, "样本列数: " + line);
            int action = Integer.parseInt(cols[4]);
            assertTrue(action == 0 || action == 1, "动作下标越界: " + action);
            assertEquals(1.0, Double.parseDouble(cols[5]), 1e-9, "CartPole 奖励恒为 1");
            assertTrue("0".equals(cols[10]) || "1".equals(cols[10]), "terminated 标志非法: " + cols[10]);
        }
    }

    @Test
    void collectAppendsWithoutDuplicateHeader() throws Exception {
        Path out = modelDir.resolve(meta.getDataFile());
        int first = ExperienceCollector.collect(meta, modelDir, 1.0, 2, out, 1);
        int second = ExperienceCollector.collect(meta, modelDir, 1.0, 2, out, 2);

        List<String> lines = Files.readAllLines(out);
        assertEquals(first + second + 1, lines.size(), "追加采样不应重复写表头");
        assertEquals(1, lines.stream().filter(l -> l.startsWith("s0,")).count());
    }
}
