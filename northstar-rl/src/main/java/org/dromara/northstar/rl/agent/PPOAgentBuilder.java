package org.dromara.northstar.rl.agent;

public class PPOAgentBuilder {
    private String indicatorSymbol;
    private String agentName = "ppo";
    private int stateDim = 4;
    private int actionDim = 3;
    private double lrActor = 0.0003;
    private double lrCritic = 0.0003;
    private double gamma = 0.99;
    private int kEpochs = 4;
    private double epsClip = 0.2;
    private boolean isTrain = false;
    private String modelVersion = "1";

    public PPOAgentBuilder() {}

    public PPOAgentBuilder indicatorSymbol(String indicatorSymbol) {
        this.indicatorSymbol = indicatorSymbol;
        return this;
    }

    public PPOAgentBuilder agentName(String agentName) {
        this.agentName = agentName;
        return this;
    }

    public PPOAgentBuilder stateDim(int stateDim) {
        this.stateDim = stateDim;
        return this;
    }

    public PPOAgentBuilder actionDim(int actionDim) {
        this.actionDim = actionDim;
        return this;
    }

    public PPOAgentBuilder lrActor(double lrActor) {
        this.lrActor = lrActor;
        return this;
    }

    public PPOAgentBuilder lrCritic(double lrCritic) {
        this.lrCritic = lrCritic;
        return this;
    }

    public PPOAgentBuilder gamma(double gamma) {
        this.gamma = gamma;
        return this;
    }

    public PPOAgentBuilder kEpochs(int kEpochs) {
        this.kEpochs = kEpochs;
        return this;
    }

    public PPOAgentBuilder epsClip(double epsClip) {
        this.epsClip = epsClip;
        return this;
    }

    public PPOAgentBuilder isTrain(boolean isTrain) {
        this.isTrain = isTrain;
        return this;
    }

    public PPOAgentBuilder modelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
        return this;
    }
}
