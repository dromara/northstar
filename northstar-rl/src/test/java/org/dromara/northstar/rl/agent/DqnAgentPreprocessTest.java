package org.dromara.northstar.rl.agent;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.dromara.northstar.strategy.rl.TradeAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * DqnAgent 预处理装配测试：custom 类型的 Translator 委托与缺参快速失败。
 * 不加载真实模型文件（model.pt 缺失导致的 IOException 即"已通过 Translator 装配阶段"的证明）。
 */
class DqnAgentPreprocessTest {

    @TempDir
    Path root;

    /** 构造 custom 预处理的模型/版本目录（动作表与 TradeAction 对齐） */
    private Path customVersionDir() throws IOException {
        Path versionDir = root.resolve("models/m/versions/v1");
        Files.createDirectories(versionDir);
        Files.writeString(versionDir.getParent().getParent().resolve("metadata.json"), """
                {"name":"m","algo":"DQN","state_dim":3,"action_dim":3,
                 "actions":["HOLD","BUY","SELL"],"preprocessing":{"type":"custom"},
                 "spec_hash":"x","latest_version":1}""");
        Files.writeString(versionDir.resolve("metadata.json"), """
                {"version":1,"parent_version":0,"weight_inherited":false,"spec_hash":"x",
                 "preprocessing":{"type":"custom"},"training":{"rounds":1,"hyper_params":{},"seed":1}}""");
        return versionDir;
    }

    @Test
    void customTypeWithoutPreprocessorFailsFast() throws Exception {
        Path versionDir = customVersionDir();
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> DqnAgent.fromVersionDir(versionDir, TradeAction.class));
        assertTrue(e.getMessage().contains("custom"));
    }

    @Test
    void customTypeWithPreprocessorPassesTranslatorStage() throws Exception {
        Path versionDir = customVersionDir();
        // 预处理器被接受：装配通过 Translator 阶段，失败点推进到模型加载（model.pt 不存在，具体异常类型取决于 DJL 引擎，但绝不是 Translator 拒配）
        Exception e = assertThrows(Exception.class,
                () -> DqnAgent.fromVersionDir(versionDir, TradeAction.class, raw -> raw));
        assertFalse(e instanceof IllegalStateException, "不应再是 Translator 拒配: " + e);
    }

    @Test
    void nonCustomTypeIgnoresInjectedPreprocessor() throws Exception {
        Path versionDir = root.resolve("models/m/versions/v1");
        Files.createDirectories(versionDir);
        Files.writeString(versionDir.getParent().getParent().resolve("metadata.json"), """
                {"name":"m","algo":"DQN","state_dim":3,"action_dim":3,
                 "actions":["HOLD","BUY","SELL"],"preprocessing":{"type":"identity"},
                 "spec_hash":"x","latest_version":1}""");
        Files.writeString(versionDir.resolve("metadata.json"), """
                {"version":1,"parent_version":0,"weight_inherited":false,"spec_hash":"x",
                 "preprocessing":{"type":"identity"},"training":{"rounds":1,"hyper_params":{},"seed":1}}""");
        // identity 类型：注入的预处理器被忽略，同样推进到模型加载阶段
        Exception e = assertThrows(Exception.class,
                () -> DqnAgent.fromVersionDir(versionDir, TradeAction.class, raw -> raw));
        assertFalse(e instanceof IllegalStateException, "不应再是 Translator 拒配: " + e);
    }
}
