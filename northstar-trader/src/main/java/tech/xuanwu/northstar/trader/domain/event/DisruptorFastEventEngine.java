package tech.xuanwu.northstar.trader.domain.event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.gateway.FastEventEngine;

@Slf4j
@Component
public class DisruptorFastEventEngine implements FastEventEngine, InitializingBean, DisposableBean {

	private static ExecutorService executor = Executors.newCachedThreadPool(DaemonThreadFactory.INSTANCE);

	private final Map<EventHandler<FastEvent>, BatchEventProcessor<FastEvent>> handlerProcessorMap = new ConcurrentHashMap<>();

	private Disruptor<FastEvent> disruptor;

	private RingBuffer<FastEvent> ringBuffer;

	@Value("${event.engine.strategy}")
	private String waitStrategy;

	private ExceptionHandler<FastEvent> commonExceptionHandler = new ExceptionHandler<>() {

		@Override
		public void handleEventException(Throwable ex, long sequence, FastEvent event) {
			log.warn("事件异常：事件类型【"+event.getEventType()+"】", ex);
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
	
	
	@Override
	public void afterPropertiesSet() throws Exception {
		if ("BusySpinWaitStrategy".equals(waitStrategy)) {
			disruptor = new Disruptor<FastEvent>(new FastEventFactory(), 65536, DaemonThreadFactory.INSTANCE,
					ProducerType.MULTI, new BusySpinWaitStrategy());
		} else if ("SleepingWaitStrategy".equals(waitStrategy)) {
			disruptor = new Disruptor<FastEvent>(new FastEventFactory(), 65536, DaemonThreadFactory.INSTANCE,
					ProducerType.MULTI, new SleepingWaitStrategy());
		} else if ("BlockingWaitStrategy".equals(waitStrategy)) {
			disruptor = new Disruptor<FastEvent>(new FastEventFactory(), 65536, DaemonThreadFactory.INSTANCE,
					ProducerType.MULTI, new BlockingWaitStrategy());
		} else {
			disruptor = new Disruptor<FastEvent>(new FastEventFactory(), 65536, DaemonThreadFactory.INSTANCE,
					ProducerType.MULTI, new YieldingWaitStrategy());
		}
		ringBuffer = disruptor.start();
		log.info("启动事件引擎");
	}

	@Override
	public synchronized void addHandler(FastEventHandler handler) {
		BatchEventProcessor<FastEvent> processor;
		processor = new BatchEventProcessor<FastEvent>(ringBuffer, ringBuffer.newBarrier(), handler);
		processor.setExceptionHandler(commonExceptionHandler);
		ringBuffer.addGatingSequences(processor.getSequence());
		executor.execute(processor);
		handlerProcessorMap.put(handler, processor);
	}

	@Override
	public void removeHandler(FastEventHandler handler) {
		if (handlerProcessorMap.containsKey(handler)) {
			BatchEventProcessor<FastEvent> processor = handlerProcessorMap.get(handler);
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
	public void emitEvent(EventType eventType, String event, Object obj) {
		long sequence = ringBuffer.next(); // Grab the next sequence
		try {
			FastEvent fastEvent = ringBuffer.get(sequence); // Get the entry in the Disruptor for the sequence
			fastEvent.setEventType(eventType);
			fastEvent.setEvent(event);
			fastEvent.setObj(obj);

		} finally {
			ringBuffer.publish(sequence);
		}
	}

	@Override
	public void destroy() throws Exception {
		disruptor.halt();
		
	}

}
