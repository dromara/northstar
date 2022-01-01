package xyz.redtorch.gateway.ctp.common;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.IntStream;

import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SmartGatewayConnector {

	private Set<Entry> gatewayIpOptions = new HashSet<>() {
		private static final long serialVersionUID = 1L;

		{
			/* 宏源地址 */
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
	
	protected LinkedList<Entry> endpointList = new LinkedList<>(gatewayIpOptions);
	
	public String bestEndpoint() {
		Collections.sort(endpointList);
		return endpointList.peek().endpoint;
	}
	
	public void update() {
		gatewayIpOptions.parallelStream().forEach(Entry::test);
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
				log.debug("[{}] 连线用时：{}毫秒", endpoint, delay);
			} catch (IOException e) {
				log.error("无法测试IP：" + endpoint, e);
			}
		}
	}
}
