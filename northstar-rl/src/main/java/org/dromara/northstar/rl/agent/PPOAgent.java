package org.dromara.northstar.rl.agent;

import com.alibaba.fastjson2.JSONObject;

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
    
    public JSONObject createAgent() {
        JSONObject jsonData = new JSONObject();
        jsonData.put("indicator_symbol", this.indicatorSymbol);
        jsonData.put("agent_name", this.agentName);
        jsonData.put("state_dim", this.stateDim);
        jsonData.put("action_dim", this.actionDim);
        jsonData.put("lr_actor", this.lrActor);
        jsonData.put("lr_critic", this.lrCritic);
        jsonData.put("gamma", this.gamma);
        jsonData.put("k_epochs", this.kEpochs);
        jsonData.put("eps_clip", this.epsClip);
        jsonData.put("is_train", this.isTrain);
        jsonData.put("model_version", this.modelVersion);
        return jsonData;
    }
}
