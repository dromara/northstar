package org.dromara.northstar.rl.agent;

import com.alibaba.fastjson.JSONObject;

public class PPOAgent implements Agent {
    public String indicatorSymbol;
    public String agentName;
    public int stateDim;
    public int actionDim;
    public double lrActor;
    public double lrCritic;
    public double gamma;
    public int kEpochs;
    public double epsClip;
    public boolean isTrain;
    public String modelVersion;

    public PPOAgent(PPOAgentDescription agentDescription) {
        this.indicatorSymbol = agentDescription.getIndicatorSymbol();
        this.agentName = agentDescription.getAgentName();
        this.stateDim = agentDescription.getStateDim();
        this.actionDim = agentDescription.getActionDim();
        this.lrActor = agentDescription.getLrActor();
        this.lrCritic = agentDescription.getLrCritic();
        this.gamma = agentDescription.getGamma();
        this.kEpochs = agentDescription.getKEpochs();
        this.epsClip = agentDescription.getEpsClip();
        this.isTrain = agentDescription.isTrain();
    }
    
    @Override
    public JSONObject createAgent() {
        JSONObject agentData = new JSONObject();
        agentData.put("indicatorSymbol", this.indicatorSymbol);
        agentData.put("agentName", this.agentName);
        agentData.put("stateDim", this.stateDim);
        agentData.put("actionDim", this.actionDim);
        agentData.put("lrActor", this.lrActor);
        agentData.put("lrCritic", this.lrCritic);
        agentData.put("gamma", this.gamma);
        agentData.put("kEpochs", this.kEpochs);
        agentData.put("epsClip", this.epsClip);
        agentData.put("isTrain", this.isTrain);
        return agentData;
    }
}
