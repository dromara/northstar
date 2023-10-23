package org.dromara.northstar.rl.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Data
public class PPOAgentDescription {

    @Builder.Default
    private String modelPath = "hello";

    @Builder.Default
    private String indicatorSymbol = "rb0000@SHFE@FUTURES";

    @Builder.Default
    private String agentName = "ppo";

    @Builder.Default
    private int stateDim = 4;

    @Builder.Default
    private int actionDim = 3;

    @Builder.Default
    private double lrActor = 0.0003;

    @Builder.Default
    private double lrCritic = 0.0003;

    @Builder.Default
    private double gamma = 0.99;

    @Builder.Default
    private int kEpochs = 4;

    @Builder.Default
    private double epsClip = 0.2;

    @Builder.Default
    private boolean isTrain = true;

    @Builder.Default
    private String modelVersion = "1";
}
