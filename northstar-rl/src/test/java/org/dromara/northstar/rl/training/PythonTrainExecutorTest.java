package org.dromara.northstar.rl.training;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * PythonTrainExecutor 单测：stdout 结构化 token 解析（parseLine）与前置校验。
 * 不起真实 Python 进程；非零退出码路径依赖子进程行为，不在此覆盖。
 */
class PythonTrainExecutorTest {

    @TempDir
    Path trainingDir;

    @Test
    void parsesStandardResultTokens() {
        PythonTrainExecutor.StdoutResult acc = new PythonTrainExecutor.StdoutResult();
        PythonTrainExecutor.parseLine("LOSS=0.123 TRAINED_STEPS=300 BUFFER_SIZE=5200", acc);

        assertEquals(0.123, acc.loss, 1e-9);
        assertEquals(300, acc.trainedSteps);
        assertEquals(5200, acc.bufferSize);
    }

    @Test
    void parsesNanLoss() {
        PythonTrainExecutor.StdoutResult acc = new PythonTrainExecutor.StdoutResult();
        PythonTrainExecutor.parseLine("LOSS=nan TRAINED_STEPS=0 BUFFER_SIZE=120", acc);

        assertTrue(Double.isNaN(acc.loss), "LOSS=nan 应解析为 NaN（该轮无有效梯度更新）");
        assertEquals(0, acc.trainedSteps);
        assertEquals(120, acc.bufferSize);

        PythonTrainExecutor.parseLine("LOSS=NaN", acc);
        assertTrue(Double.isNaN(acc.loss), "大小写不敏感");
    }

    @Test
    void ignoresUnrelatedTokensAndMalformedFragments() {
        PythonTrainExecutor.StdoutResult acc = new PythonTrainExecutor.StdoutResult();
        PythonTrainExecutor.parseLine("INFO start training", acc);
        PythonTrainExecutor.parseLine("no-equals-token KEY= OTHER=1", acc);
        PythonTrainExecutor.parseLine("", acc);

        assertTrue(Double.isNaN(acc.loss), "无关 token 不影响解析结果");
        assertEquals(0, acc.trainedSteps);
        assertEquals(0, acc.bufferSize);
    }

    @Test
    void lastTokenWinsOnRepeatedKeys() {
        PythonTrainExecutor.StdoutResult acc = new PythonTrainExecutor.StdoutResult();
        PythonTrainExecutor.parseLine("LOSS=0.5", acc);
        PythonTrainExecutor.parseLine("LOSS=0.25", acc);

        assertEquals(0.25, acc.loss, 1e-9, "同一 key 重复输出时以最后一行为准");
    }

    @Test
    void trainRoundRejectsMissingScript() {
        PythonTrainExecutor executor = new PythonTrainExecutor("python", trainingDir);

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> executor.trainRound(Path.of("models/m"), 1, 300, null, 42));
        assertTrue(e.getMessage().contains("训练脚本不存在"), e.getMessage());
    }
}
