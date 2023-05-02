package org.dromara.northstar.data.jdbc.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@Data
@Entity
@Table(name="MODULE", indexes = {
		@Index(name="idx_moduleName", columnList = "moduleName"),
})
public class ModuleDescriptionDO {

	@Id
	private String moduleName;
	
	private String moduleDescriptionDataStr;
	
	private String moduleRuntimeDescriptionDataStr;
	
	private String moduleDealRecords;
	
	
}
