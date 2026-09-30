package org.dromara.northstar.rl.agent;

import ai.djl.util.JsonUtils;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 模型元信息（metadata.json）的 Java 映射。
 * 一个 model 目录 = 模型文件 + metadata.json + 训练数据，自描述、可独立部署。
 * Python 训练侧与 Java 推理侧都以它为初始化契约。
 */
public class ModelMetadata {

    public static final String FILE_NAME = "metadata.json";

    @SerializedName("name")
    private String name;
    @SerializedName("version")
    private int version;
    @SerializedName("state_dim")
    private int stateDim;
    @SerializedName("action_dim")
    private int actionDim;
    @SerializedName("actions")
    private String[] actions;
    @SerializedName("hidden")
    private int hidden = 128;
    @SerializedName("model_file")
    private String modelFile = "model.pt";
    @SerializedName("data_file")
    private String dataFile = "replay.csv";
    @SerializedName("preprocessing")
    private Preprocessing preprocessing = new Preprocessing();

    public static class Preprocessing {
        @SerializedName("type")
        private String type = "identity";
        @SerializedName("mean")
        private float[] mean;
        @SerializedName("std")
        private float[] std;

        public String getType() {
            return type;
        }

        public float[] getMean() {
            return mean;
        }

        public float[] getStd() {
            return std;
        }
    }

    /** 从 model 目录加载 metadata.json。 */
    public static ModelMetadata load(Path modelDir) throws IOException {
        Path file = modelDir.resolve(FILE_NAME);
        try (Reader reader = Files.newBufferedReader(file)) {
            return JsonUtils.GSON.fromJson(reader, ModelMetadata.class);
        }
    }

    public String getName() {
        return name;
    }

    public int getVersion() {
        return version;
    }

    public int getStateDim() {
        return stateDim;
    }

    public int getActionDim() {
        return actionDim;
    }

    /** 动作名表：actions[i] = 动作下标 i 对应的名称，与枚举 ordinal 一一对应。 */
    public String[] getActions() {
        return actions;
    }

    public int getHidden() {
        return hidden;
    }

    public String getModelFile() {
        return modelFile;
    }

    public String getDataFile() {
        return dataFile;
    }

    public Preprocessing getPreprocessing() {
        return preprocessing;
    }
}
