package org.dromara.northstar.data.jdbc.entity;

import org.dromara.northstar.common.model.ExplorerTaskDescription;

import com.alibaba.fastjson2.JSON;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name="EXPLORER_TASK")
public class ExplorerTaskDO {

	@Id
	private String taskName;
	@Lob
	private String dataStr;
	
	public static ExplorerTaskDO convertFrom(ExplorerTaskDescription task) {
		return new ExplorerTaskDO(task.getTaskName(), JSON.toJSONString(task));
	}
	
	public ExplorerTaskDescription convertTo() {
		return JSON.parseObject(dataStr, ExplorerTaskDescription.class);
	}
}
