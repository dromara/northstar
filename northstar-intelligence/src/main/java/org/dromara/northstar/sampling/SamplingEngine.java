package org.dromara.northstar.sampling;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;

import org.dromara.northstar.ai.SampleData;
import org.dromara.northstar.ai.SamplingAware;
import org.dromara.northstar.common.ObjectManager;
import org.dromara.northstar.strategy.IModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import lombok.extern.slf4j.Slf4j;

/**
 * 监控学习数据采样器。采样器会把回调方法注入到策略中，当有K线数据时会触发回调进行采样 
 * 仅用于开发环境
 * @auth KevinHuangwl
 */
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE - 1)
@Component
@Profile("sampling")
public class SamplingEngine implements CommandLineRunner {

	@Autowired
	private ObjectManager<IModule> moduleMgr;
	
	@Override
	public void run(String... args) throws Exception {
		log.info("给模组注入数据采样器");
		moduleMgr.findAll()
			.stream()
			.filter(m -> m.getTradeStrategy() instanceof SamplingAware)
			.map(SamplingAware.class::cast)
			.forEach(sa -> {
				IModule m = (IModule) sa;
				CsvMapper mapper = new CsvMapper();
				sa.setOnBarCallback(data -> {
					File csvFile = new File(String.format("data/%s_%s.csv", m.getTradeStrategy().name(), data.unifiedSymbol()));
					CsvSchema schema = mapper.schemaFor(SampleData.class).withHeader();	
			        if (csvFile.exists() && csvFile.length() > 0) {
			            schema = schema.withoutHeader();
			        }
			        try (FileWriter fileWriter = new FileWriter(csvFile, Charset.defaultCharset());
			        		SequenceWriter seqWriter = mapper.writer(schema).writeValues(fileWriter)) {
			        		seqWriter.write(data);
			        		seqWriter.flush(); 
			           } catch (IOException e) {
			        	   log.error("", e);
			           }
				});
			});
	}
}


