package org.dromara.northstar.strategy.example;

import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;

import com.alibaba.fastjson.JSONObject;

import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

@StrategicComponent(RLStrategy.NAME)
public class RLStrategy implements TradeStrategy {

    protected static final String NAME = "示例-RL策略";

    private InitParams params;

    public static class InitParams {

    }

    @Override
    public void onOrder(OrderField order) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onOrder'");
    }

    @Override
    public void onTrade(TradeField trade) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onTrade'");
    }

    @Override
    public void setContext(IModuleContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setContext'");
    }

    @Override
    public DynamicParams getDynamicParams() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDynamicParams'");
    }

    @Override
    public void initWithParams(DynamicParams params) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'initWithParams'");
    }

    @Override
    public JSONObject getStoreObject() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getStoreObject'");
    }

    @Override
    public void setStoreObject(JSONObject storeObj) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setStoreObject'");
    }

    @Override
    public void onTick(TickField tick) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onTick'");
    }

    @Override
    public void onMergedBar(BarField bar) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onMergedBar'");
    }
}