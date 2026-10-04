package org.dromara.northstar.data.jdbc.entity;

import org.dromara.northstar.app.rl.RLTrainingTaskRecord;

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
@Table(name = "RL_TRAINING_TASK")
public class RLTrainingTaskDO {

	@Id
	private String taskId;
	@Lob
	private String dataStr;

	public static RLTrainingTaskDO convertFrom(RLTrainingTaskRecord record) {
		return new RLTrainingTaskDO(record.getTaskId(), JSON.toJSONString(record));
	}

	public RLTrainingTaskRecord convertTo() {
		return JSON.parseObject(dataStr, RLTrainingTaskRecord.class);
	}
}
