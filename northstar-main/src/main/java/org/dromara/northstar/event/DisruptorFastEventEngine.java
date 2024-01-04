package org.dromara.northstar.event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.utils.CommonUtils;
import org.springframework.beans.factory.DisposableBean;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.TimeoutBlockingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.CommonStatusEnum;
import xyz.redtorch.pb.CoreField.NoticeField;

/**
 * 核心事件引擎
 * @author KevinHuangwl
 *
 */
@Slf4j
public class DisruptorFastEventEngine implements FastEventEngine, DisposableBean {

	private static final ExecutorService executor = Executors.newThreadPerTaskExecutor(CommonUtils.virtualThreadFactory(DisruptorFastEventEngine.class));
	private static final int BUF_SIZE = 65536;
	
	private final Map<EventHandler<NorthstarEvent>, BatchEventProcessor<NorthstarEvent>> handlerProcessorMap = new ConcurrentHashMap<>();

	private Disruptor<NorthstarEvent> disruptor;

	private RingBuffer<NorthstarEvent> ringBuffer;
	
	private ExceptionHandler<NorthstarEvent> commonExceptionHandler = new ExceptionHandler<>() {

		@Override
		public void handleEventException(Throwable ex, long sequence, NorthstarEvent event) {
			log.warn("事件异常：事件类型【"+event+"】", ex);
			emitEvent(NorthstarEventType.NOTICE, NoticeField.newBuilder()
					.setContent(ex.toString())
					.setStatus(CommonStatusEnum.COMS_ERROR)
					.setTimestamp(System.currentTimeMillis())
					.build());
		}

		@Override
		public void handleOnStartException(Throwable ex) {
			log.warn("事件启动异常", ex);
		}

		@Override
		public void handleOnShutdownException(Throwable ex) {
			log.warn("事件中止异常", ex);			
		}
		
	};
	
	public DisruptorFastEventEngine(WaitStrategyEnum strategy) throws Exception {
		WaitStrategy s = (WaitStrategy) strategy.getStrategyClass().getDeclaredConstructor().newInstance();
		disruptor = new Disruptor<>(new NorthstarEventFactory(), BUF_SIZE, DaemonThreadFactory.INSTANCE,
				ProducerType.MULTI, s);
		ringBuffer = disruptor.start();
		log.info("启动事件引擎");
	}
	
	@Override
	public void addHandler(NorthstarEventDispatcher handler) {
		log.debug("加载：{}", handler);
		BatchEventProcessor<NorthstarEvent> processor = new BatchEventProcessor<>(ringBuffer, ringBuffer.newBarrier(), handler);
		processor.setExceptionHandler(commonExceptionHandler);
		ringBuffer.addGatingSequences(processor.getSequence());
		executor.execute(processor);
		handlerProcessorMap.put(handler, processor);
	}

	@Override
	public void removeHandler(NorthstarEventDispatcher handler) {
		if (handlerProcessorMap.containsKey(handler)) {
			BatchEventProcessor<NorthstarEvent> processor = handlerProcessorMap.get(handler);
			// Remove a processor.
			// Stop the processor
			processor.halt();
			// Wait for shutdown the complete
//			try {
//				handler.awaitShutdown();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//				log.error("关闭时发生异常", e);
//			}
			// Remove the gating sequence from the ring buffer
			ringBuffer.removeGatingSequence(processor.getSequence());
			handlerProcessorMap.remove(handler);
		} else {
			log.warn("未找到Processor,无法移除");
		}

	}

	@Override
	public void destroy() throws Exception {
		disruptor.halt();
	}

	@Override
	public void emitEvent(NorthstarEventType event, Object obj) {
		long sequence = ringBuffer.next(); // Grab the next sequence
		try {
			NorthstarEvent fastEvent = ringBuffer.get(sequence); // Get the entry in the Disruptor for the sequence
			fastEvent.setEvent(event);
			fastEvent.setData(obj);

		} finally {
			ringBuffer.publish(sequence);
		}
	}
	
	public enum WaitStrategyEnum {
		BlockingWaitStrategy(BlockingWaitStrategy.class),
		BusySpinWaitStrategy(BusySpinWaitStrategy.class),
		SleepingWaitStrategy(SleepingWaitStrategy.class),
		TimeoutBlockingWaitStrategy(TimeoutBlockingWaitStrategy.class),
		YieldingWaitStrategy(YieldingWaitStrategy.class);
		
		private Class<?> clz;
		private WaitStrategyEnum(Class<?> clz){
			this.clz = clz;
		}
		
		public Class<?> getStrategyClass(){
			return clz;
		}
	}

}
