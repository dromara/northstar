package tech.quantit.northstar.common.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.model.ContractDefinition;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

public class ContractDefinitionReader {

	public List<ContractDefinition> load(File definitionFilePath) throws IOException{
		List<ContractDefinition> resultList = new LinkedList<>();
		try(BufferedReader bis = new BufferedReader(new FileReader(definitionFilePath, Charset.forName("UTF-8")))){
			String headerLine = bis.readLine();
			Map<String, Integer> headerMap = getHeaderIndexMap(headerLine);
			String line = null;
			while((line = bis.readLine()) != null) {
				String[] tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)",-1);
				Double commission = StringUtils.isEmpty(tokens[headerMap.get("commission")]) ? 0D : Double.parseDouble(tokens[headerMap.get("commission")]);
				Double commissionInBP = StringUtils.isEmpty(tokens[headerMap.get("commissionInBP")]) ? 0D : Double.parseDouble(tokens[headerMap.get("commissionInBP")]); 
				String ptnStr = tokens[headerMap.get("symbolPattern")].replaceAll("^\"(.+)\"$", "$1");
				resultList.add(ContractDefinition.builder()
						.gatewayType(GatewayType.valueOf(tokens[headerMap.get("gatewayType")]))
						.name(tokens[headerMap.get("name")])
						.productClass(ProductClassEnum.valueOf(tokens[headerMap.get("class")]))
						.symbolPattern(Pattern.compile(ptnStr))
						.commissionInPrice(commission)
						.commissionInBasePoint(commissionInBP)
						.build());
			}
		}
		
		return resultList;
	}
	
	private Map<String, Integer> getHeaderIndexMap(String headerLine){
		Map<String, Integer> keyIndexMap = new HashMap<>();
		String[] tokens = headerLine.split(",");
		for(int i=0; i<tokens.length; i++) {
			keyIndexMap.put(tokens[i], i);
		}
		return keyIndexMap;
	}
}
