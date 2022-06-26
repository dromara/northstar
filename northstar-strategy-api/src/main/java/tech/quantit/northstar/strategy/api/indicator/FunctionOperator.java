package tech.quantit.northstar.strategy.api.indicator;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class FunctionOperator {
	
	
	public static Mono<Double> add(Mono<Double> fn1, Mono<Double> fn2) {
		return Flux.concat(fn1, fn2).reduce((a, b) -> a + b);
	}
	
}
