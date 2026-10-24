package org.dromara.northstar.strategy.example;

import java.util.List;

import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.helper.HHVIndicator;
import org.dromara.northstar.indicator.helper.LLVIndicator;
import org.dromara.northstar.indicator.helper.SimpleValueIndicator;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.trend.MAIndicator;
import org.dromara.northstar.rl.agent.DqnAgent;
import org.dromara.northstar.rl.agent.RLModelLoader;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.IModuleStrategyContext;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.rl.EpisodeEnd;
import org.dromara.northstar.strategy.rl.NormalizerType;
import org.dromara.northstar.strategy.rl.PreprocessType;
import org.dromara.northstar.strategy.rl.RLFeatureSpec;
import org.dromara.northstar.strategy.rl.RLModelSpec;
import org.dromara.northstar.strategy.rl.RLRewardContext;
import org.dromara.northstar.strategy.rl.RLStrategy;
import org.dromara.northstar.strategy.rl.TradeAction;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;

/**
 * RL 趋势跟踪示例策略：演示 RLStrategy 接口的标准实现。
 *
 * 模型形状（6 维状态 × 3 动作）由本类定义，训练与实盘共用同一份
 * observe/shouldObserve/onAction 代码路径，保证训推一致。
 *
 * 语义约定：BUY = 目标仓位做多（持空先平空再开多）；SELL = 目标仓位做空；HOLD = 保持。
 *
 * ## 风险提示：该策略仅作技术演示，据此交易，风险自担 ##
 */
@StrategicComponent(RlTestStrategy.NAME)
public class RlTestStrategy extends AbstractStrategy implements RLStrategy {

	protected static final String NAME = "RL测试策略";

	private InitParams params;

	private Indicator close;	// 收盘价（供收益率/区间特征回溯）
	private Indicator maFast;
	private Indicator maSlow;
	private Indicator hhv;
	private Indicator llv;

	private DqnAgent<TradeAction> liveAgent;	// 实盘推理（训练时由环境驱动，无需加载）

	public static class InitParams extends DynamicParams {
		@Setting(label = "触发灵敏度", type = FieldType.NUMBER, order = 10, unit = "%")
		private double observeThreshold = 0.2;	// 单bar涨跌幅超过该阈值（百分比）即触发决策点

		@Setting(label = "推理模型版本号", type = FieldType.NUMBER, order = 20, placeholder = "正整数指定版本；0 表示不启用推理", required = false)
		private int modelVersion = 0;
	}

	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		this.params = (InitParams) params;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String modelName() {
		return "dqn-demo";	// 网络结构身份：Python registry.py 以同名 key 注册 DQN 网络构建函数
	}

	@Override
	public RLModelSpec rlSpec() {
		return new RLModelSpec(List.of(
				new RLFeatureSpec("ret1", NormalizerType.RATIO, "单bar收益率"),
				new RLFeatureSpec("ret5", NormalizerType.RATIO, "5bar收益率"),
				new RLFeatureSpec("maDev", NormalizerType.RATIO, "收盘价对慢线偏离"),
				new RLFeatureSpec("trendDev", NormalizerType.RATIO, "快慢线偏离"),
				new RLFeatureSpec("rangePos", NormalizerType.RATIO, "收盘价在近期区间的相对位置"),
				new RLFeatureSpec("position", NormalizerType.NONE, "当前持仓方向(-1/0/1)")),
				TradeAction.class, PreprocessType.IDENTITY);
	}

	@Override
	public int rlWarmupBars() {
		return 60;	// max(慢线20, 区间14) × 安全系数
	}

	@Override
	protected void initIndicators() {
		Contract contract = ctx.bindedContracts().get(0);
		close = new SimpleValueIndicator(Configuration.builder()
				.indicatorName("C").contract(contract).cacheLength(16).visible(false).build());
		maFast = new MAIndicator(Configuration.builder()
				.indicatorName("MA5").contract(contract).visible(false).build(), close, 5);
		maSlow = new MAIndicator(Configuration.builder()
				.indicatorName("MA20").contract(contract).visible(false).build(), close, 20);
		hhv = new HHVIndicator(Configuration.builder()
				.indicatorName("HHV14").contract(contract).visible(false).build(), close);
		llv = new LLVIndicator(Configuration.builder()
				.indicatorName("LLV14").contract(contract).visible(false).build(), close);
		ctx.registerIndicator(maFast);
		ctx.registerIndicator(maSlow);
		ctx.registerIndicator(hhv);
		ctx.registerIndicator(llv);
	}

	@Override
	public float[] observe(IModuleStrategyContext ctx, Contract contract) {
		double c0 = close.value(0);
		double c1 = close.value(-1);
		double c5 = close.value(-5);
		double slow = maSlow.value(0);
		double hi = hhv.value(0);
		double lo = llv.value(0);
		return new float[] {
				(float) (c0 / c1 - 1),					// ret1
				(float) (c0 / c5 - 1),					// ret5
				(float) (c0 / slow - 1),				// maDev
				(float) (maFast.value(0) / slow - 1),	// trendDev
				(float) ((c0 - lo) / (hi - lo + 1e-9)),	// rangePos
				positionFlag()							// position
		};
	}

	@Override
	public boolean shouldObserve(IModuleStrategyContext ctx, Contract contract) {
		if (!close.isReady() || !maSlow.isReady() || !hhv.isReady() || !llv.isReady()) {
			return false;
		}
		double ret1 = close.value(0) / close.value(-1) - 1;
		if (Math.abs(ret1) * 100 >= params.observeThreshold) {
			return true;	// 波动超过灵敏度阈值
		}
		// 持仓且波段动能转弱（5bar收益与持仓方向相反）时触发重新观测
		double ret5 = close.value(0) / close.value(-5) - 1;
		float pos = positionFlag();
		return pos != 0 && Math.signum(ret5) == -Math.signum(pos);
	}

	@Override
	public void onAction(TradeAction action, IModuleStrategyContext ctx, Contract contract) {
		switch (action) {
			case BUY -> {
				if (positionFlag() < 0) {	// 平空
					submit(ctx, contract, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Close);
				}
				if (positionFlag() <= 0) {	// 开多
					submit(ctx, contract, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open);
				}
			}
			case SELL -> {
				if (positionFlag() > 0) {	// 平多
					submit(ctx, contract, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close);
				}
				if (positionFlag() >= 0) {	// 开空
					submit(ctx, contract, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open);
				}
			}
			case HOLD -> { /* 观望 */ }
		}
	}

	@Override
	public double reward(RLRewardContext ctx) {
		// 区间收益率（百分点）为主成分；权益击穿的真失败施加重罚
		double r = ctx.intervalProfit() / ctx.prevEquity() * 100;
		return ctx.episodeEnd() == EpisodeEnd.TERMINATED ? r - 10 : r;
	}

	@Override
	public void onTick(Tick tick) {
		// RL 策略不逐 tick 响应，决策由 onMergedBar 驱动
	}

	@Override
	public void onMergedBar(Bar bar) {
		// 实盘路径：与训练环境执行完全相同的钩子代码路径（训推一致）
		if (params.modelVersion > 0 && shouldObserve(ctx, bar.contract())) {
			try {
				TradeAction action = liveAgent().act(observe(ctx, bar.contract()));
				onAction(action, ctx, bar.contract());
			} catch (Exception e) {
				throw new IllegalStateException("RL 推理失败", e);
			}
		}
	}

	private synchronized DqnAgent<TradeAction> liveAgent() {
		if (liveAgent == null) {
			// 模型名来自 modelName()，版本目录由框架按目录契约自动解析，并校验 specHash（策略演进防护）
			liveAgent = RLModelLoader.loadLiveAgent(this, TradeAction.class, params.modelVersion);
		}
		return liveAgent;
	}

	private float positionFlag() {
		if (ctx.getState() == org.dromara.northstar.common.constant.ModuleState.HOLDING_LONG) {
			return 1;
		}
		if (ctx.getState() == org.dromara.northstar.common.constant.ModuleState.HOLDING_SHORT) {
			return -1;
		}
		return 0;
	}

	private void submit(IModuleStrategyContext ctx, Contract contract, DirectionEnum dir, OffsetFlagEnum offset) {
		ctx.submitOrderReq(SubmitOrderReq.builder()
				.contract(contract)
				.direction(dir)
				.offsetFlag(offset)
				.volume(ctx.getDefaultVolume())
				.build());
	}
}
