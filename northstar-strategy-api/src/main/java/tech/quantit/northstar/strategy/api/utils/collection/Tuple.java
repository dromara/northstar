package tech.quantit.northstar.strategy.api.utils.collection;

/**
 * 双元素元组
 * @author KevinHuangwl
 *
 * @param <T1>
 * @param <T2>
 */
public record Tuple<T1, T2>(T1 t1, T2 t2) {

	public static <T1,T2> Tuple<T1,T2> of(T1 t1, T2 t2){
		return new Tuple<>(t1, t2);
	}
}
