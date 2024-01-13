package org.dromara.northstar.ai.sampling;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.dromara.northstar.ai.SampleData;

import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SampleDataWriter {

	private File destFile;
	private CsvMapper mapper = new CsvMapper();
	private CsvSchema schema = mapper.schemaFor(SampleData.class).withHeader();
	
    public SampleDataWriter(File destFile) {
        this.destFile = destFile;
        try {
			FileUtils.forceMkdirParent(destFile);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
    }

    public void append(SampleData data) {
    	boolean isAppend = false;
    	if(destFile.exists()) {
    		isAppend = true;
    		schema = schema.withoutHeader();
    	}
    	try (FileWriter fileWriter = new FileWriter(destFile, isAppend);
        		SequenceWriter seqWriter = mapper.writer(schema).writeValues(fileWriter)) {
        		seqWriter.write(data);
        		seqWriter.flush(); 
           } catch (IOException e) {
        	   log.error("", e);
           }
    }
}
