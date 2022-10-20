package xyz.redtorch.gateway.ctp.common;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SmartGatewayConnector {
	
	private static final String HY_PRI = "1080";	//宏源主席
	private static final String HY_SEC = "2070";	//宏源次席
	private static final String HY_SIM = "3070";	//宏源仿真
	private static final String PA_PRI = "5200";	//平安主席

	private Set<Entry> gatewayIpOptions = new HashSet<>() {
		private static final long serialVersionUID = 1L;
		{
			/* 宏源地址 主席地址 */
			add(new Entry("180.169.112.52"));
			add(new Entry("180.169.112.53"));
			add(new Entry("180.169.112.54"));
			add(new Entry("180.169.112.55"));
			add(new Entry("106.37.231.6"));
			add(new Entry("106.37.231.7"));
			add(new Entry("140.206.101.109"));
			add(new Entry("140.206.101.110"));
			add(new Entry("140.207.168.9"));
			add(new Entry("140.207.168.10"));
			add(new Entry("111.205.217.41"));
			add(new Entry("111.205.217.40"));
		}
	};
	
	private Set<Entry> gatewayIpOptions2 = new HashSet<>() {
		private static final long serialVersionUID = 1L;
		{
			/* 宏源地址 次席地址 */
			add(new Entry("180.169.112.50"));
			add(new Entry("180.169.112.51"));
			add(new Entry("140.206.101.107"));
			add(new Entry("140.206.101.108"));
		}
	};
	
	private Set<Entry> gatewayIpOptions3 = new HashSet<>() {
		private static final long serialVersionUID = 1L;
		{
			/* 平安地址 主席地址 */
			add(new Entry("101.226.254.58"));
			add(new Entry("101.226.253.171"));
			add(new Entry("140.206.244.198"));
			add(new Entry("140.206.242.43"));
		}
	};
	
	
	protected LinkedList<Entry> endpointHYPRI = new LinkedList<>(gatewayIpOptions);
	protected LinkedList<Entry> endpointHYSEC = new LinkedList<>(gatewayIpOptions2);
	protected LinkedList<Entry> endpointPAPRI = new LinkedList<>(gatewayIpOptions3);
	protected LinkedList<Entry> endpointHYSIM = new LinkedList<>(List.of(new Entry("120.136.162.186")));
	
	ExecutorService exec = Executors.newCachedThreadPool();
	
	
	public SmartGatewayConnector(){
		update();
	}
	
	public String bestEndpoint(String brokerId) {
		LinkedList<Entry> endpoints = switch(brokerId) {
		case HY_PRI -> endpointHYPRI;
		case HY_SEC -> endpointHYSEC;
		case HY_SIM -> endpointHYSIM;
		case PA_PRI -> endpointPAPRI;
		default -> throw new IllegalArgumentException("未知BrokerID：" + brokerId);
		};
		Collections.sort(endpoints);
		return endpoints.peek().endpoint;
	}
	
	public void update() {
		gatewayIpOptions.stream().forEach(Entry::test);
		gatewayIpOptions2.stream().forEach(Entry::test);
		gatewayIpOptions3.stream().forEach(Entry::test);
	}
	
	@EqualsAndHashCode
	private class Entry implements Comparable<Entry>{
		private String endpoint;
		private int delay = Integer.MAX_VALUE;
		
		Entry(String endpoint){
			this.endpoint = endpoint;
		}
		
		@Override
		public int compareTo(Entry o) {
			return delay < o.delay ? -1 : 1;
		}
		
		public void test() {
			exec.execute(() -> {				
				try {				
					InetAddress geek = InetAddress.getByName(endpoint);
					int[] testResults = new int[10];
					for(int i=0; i<10; i++) {					
						long startTime = System.currentTimeMillis();
						if(geek.isReachable(5000)) {
							testResults[i] = (int) (System.currentTimeMillis() - startTime);
						} else {
							testResults[i] = 5000;
						}
					}
					delay = IntStream.of(testResults).sum() / 10;
					log.trace("[{}] 连线用时：{}毫秒", endpoint, delay);
				} catch (IOException e) {
					log.error("无法测试IP：" + endpoint, e);
				}
			});
		}
	}
}
