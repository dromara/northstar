package org.dromara.northstar.rl.agent;

import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDList;
import ai.djl.translate.Batchifier;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.dromara.northstar.rl.model.RLModelMetadata;
import org.dromara.northstar.rl.model.RLVersionMetadata;
import org.dromara.northstar.strategy.rl.RLStatePreprocessor;
import org.dromara.northstar.strategy.rl.RLStrategy;

/**
 * 通用 DQN 推理 Agent：本质上是预训练模型的调用壳。
 *
 * 构造时传入：
 * <ul>
 *   <li>模型文件路径（TorchScript / .pt）</li>
 *   <li>输入向量维度 inputDim、输出向量（动作数）维度 actionDim</li>
 *   <li>动作枚举类（枚举 ordinal = 模型的动作下标）</li>
 *   <li>可选：自定义 {@link Translator}——DJL 官方的预处理抽象，
 *       processInput/processOutput 即前处理/后处理钩子</li>
 * </ul>
 *
 * 输出侧固定为 argmax：取 Q 值最大的下标，返回对应枚举值。
 * 换业务场景 = 换模型文件 + 换维度/枚举描述 + 换 Translator，Agent 本身不变。
 *
 * @param <A> 动作枚举类型
 */
public class DqnAgent<A extends Enum<A>> implements AutoCloseable {

    /**
     * 默认 Translator：float[] 观测 <-> NDList 的直通转换。
     * 不同模型的输入预处理不同（归一化、特征拼接等），
     * 继承本类并覆盖 {@link #preprocess} 即可定制。
     */
    public static class FloatVectorTranslator implements Translator<float[], float[]> {

        @Override
        public NDList processInput(TranslatorContext ctx, float[] input) {
            // ctx.getNDManager() 由 Predictor 按次管理，无需手动关闭
            return new NDList(ctx.getNDManager().create(preprocess(input)).expandDims(0));
        }

        /** 预处理钩子：默认直通，子类按需覆盖（归一化、特征工程等）。 */
        protected float[] preprocess(float[] rawState) {
            return rawState;
        }

        @Override
        public float[] processOutput(TranslatorContext ctx, NDList list) {
            return list.singletonOrThrow().toFloatArray();
        }

        @Override
        public Batchifier getBatchifier() {
            return null; // 逐条推理
        }
    }

    /**
     * 标准化 Translator 工厂：x' = (x - mean) / std。
     * mean/std 必须来自训练侧统计，训练与推理必须使用同一份参数。
     */
    public static Translator<float[], float[]> standardizing(float[] mean, float[] std) {
        if (mean.length != std.length) {
            throw new IllegalArgumentException("mean 与 std 长度不一致");
        }
        return new FloatVectorTranslator() {
            @Override
            protected float[] preprocess(float[] raw) {
                if (raw.length != mean.length) {
                    throw new IllegalArgumentException(
                            "观测维度 " + raw.length + " 与标准化参数维度 " + mean.length + " 不一致");
                }
                float[] out = new float[raw.length];
                for (int i = 0; i < raw.length; i++) {
                    out[i] = (raw[i] - mean[i]) / std[i];
                }
                return out;
            }
        };
    }

    /**
     * 自定义预处理 Translator 工厂：委托策略提供的 {@link RLStatePreprocessor}。
     * 框架额外守住保长契约（输出维度必须与输入一致），长度不符立即报错而非留给 NDArray 层。
     */
    public static Translator<float[], float[]> preprocessing(RLStatePreprocessor preprocessor) {
        return new FloatVectorTranslator() {
            @Override
            protected float[] preprocess(float[] raw) {
                float[] out = preprocessor.apply(raw);
                if (out.length != raw.length) {
                    throw new IllegalStateException(
                            "自定义预处理输出维度 " + out.length + " 与观测维度 " + raw.length + " 不一致（须保持长度不变）");
                }
                return out;
            }
        };
    }

    private final int inputDim;
    private final int actionDim;
    private final A[] actions;

    private final Model model;
    private final Predictor<float[], float[]> predictor;

    /** 使用默认 Translator（输入直通）。 */
    public DqnAgent(Path modelPath, int inputDim, int actionDim, Class<A> actionType)
            throws IOException, MalformedModelException {
        this(modelPath, inputDim, actionDim, actionType, new FloatVectorTranslator());
    }

    /**
     * 从模型版本目录创建 Agent：读取模型级 metadata（维度/动作表）与版本级 metadata（预处理策略），
     * 并校验 metadata 的动作表与动作枚举逐一对应（名称与顺序都必须一致）。
     *
     * @param versionDir 版本目录（models/{modelId}/versions/v{n}，含 model.pt 与版本 metadata.json）
     */
    public static <A extends Enum<A>> DqnAgent<A> fromVersionDir(Path versionDir, Class<A> actionType)
            throws IOException, MalformedModelException {
        return fromVersionDir(versionDir, actionType, null);
    }

    /**
     * {@link #fromVersionDir(Path, Class)} 的 custom 预处理变体。
     *
     * 仅当版本 metadata 声明 {@code preprocessing.type = "custom"} 时使用 customPreprocessor：
     * 变换逻辑只存在于策略 Java 代码中，metadata 无法重建，必须由调用方（RLAgent）注入策略的
     * {@link RLStrategy#rlPreprocessor()} 实例。metadata 为其他类型时该参数被忽略（identity/standardize
     * 的变换可从 metadata 重建）。
     *
     * @param customPreprocessor 策略提供的自定义预处理器；metadata 声明 custom 时为必填，否则快速失败
     */
    public static <A extends Enum<A>> DqnAgent<A> fromVersionDir(Path versionDir, Class<A> actionType,
            RLStatePreprocessor customPreprocessor) throws IOException, MalformedModelException {
        Path modelDir = versionDir.getParent().getParent();
        RLModelMetadata meta = RLModelMetadata.load(modelDir);
        RLVersionMetadata verMeta = RLVersionMetadata.load(versionDir);
        A[] constants = actionType.getEnumConstants();
        List<String> actions = meta.getActions();
        if (actions == null || actions.size() != constants.length) {
            throw new IllegalArgumentException(
                    "metadata 动作表（" + (actions == null ? 0 : actions.size()) + " 个）与枚举 "
                            + actionType.getSimpleName() + "（" + constants.length + " 个）数量不一致");
        }
        for (int i = 0; i < constants.length; i++) {
            if (!constants[i].name().equals(actions.get(i))) {
                throw new IllegalArgumentException(
                        "动作下标 " + i + " 不匹配：枚举为 " + constants[i].name()
                                + "，metadata 为 " + actions.get(i));
            }
        }
        RLModelMetadata.Preprocessing pp = verMeta.getPreprocessing();
        Translator<float[], float[]> translator = switch (pp.getType()) {
            case "identity" -> new FloatVectorTranslator();
            case "standardize" -> standardizing(pp.getMean(), pp.getStd());
            case "custom" -> {
                if (customPreprocessor == null) {
                    throw new IllegalStateException(
                            "模型版本声明了 custom 预处理，但未注入策略的 RLStatePreprocessor"
                                    + "（须经由 RLAgent 传入 RLStrategy.rlPreprocessor() 的实例）");
                }
                yield preprocessing(customPreprocessor);
            }
            default -> throw new IllegalArgumentException("未知的预处理类型: " + pp.getType());
        };
        return new DqnAgent<>(versionDir.resolve("model.pt"),
                meta.getStateDim(), meta.getActionDim(), actionType, translator);
    }

    public DqnAgent(Path modelPath, int inputDim, int actionDim, Class<A> actionType,
                    Translator<float[], float[]> translator) throws IOException, MalformedModelException {
        if (inputDim <= 0 || actionDim <= 0) {
            throw new IllegalArgumentException("inputDim/actionDim 必须为正数");
        }
        this.actions = actionType.getEnumConstants();
        if (actions.length != actionDim) {
            throw new IllegalArgumentException(
                    "枚举 " + actionType.getSimpleName() + " 有 " + actions.length
                            + " 个常量，与 actionDim " + actionDim + " 不一致");
        }
        this.inputDim = inputDim;
        this.actionDim = actionDim;

        this.model = Model.newInstance(modelPath.getFileName().toString(), "PyTorch");
        this.model.load(modelPath);
        this.predictor = model.newPredictor(translator);
    }

    /** 前向推理：原始观测 -> Translator 预处理 -> 模型 -> Q 值向量。 */
    public float[] qValues(float[] rawState) throws TranslateException {
        if (rawState.length != inputDim) {
            throw new IllegalArgumentException(
                    "输入维度 " + rawState.length + " 与声明的 inputDim " + inputDim + " 不一致");
        }
        float[] q = predictor.predict(rawState);
        if (q.length != actionDim) {
            throw new IllegalStateException(
                    "模型输出维度 " + q.length + " 与声明的 actionDim " + actionDim + " 不一致，请检查模型或配置");
        }
        return q;
    }

    /** 决策：原始观测 -> Q 值 -> argmax -> 动作枚举值。 */
    public A act(float[] rawState) throws TranslateException {
        float[] q = qValues(rawState);
        int best = 0;
        for (int i = 1; i < q.length; i++) {
            if (q[i] > q[best]) {
                best = i;
            }
        }
        return actions[best];
    }

    public int getInputDim() {
        return inputDim;
    }

    public int getActionDim() {
        return actionDim;
    }

    @Override
    public void close() {
        predictor.close();
        model.close();
    }
}
