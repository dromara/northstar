package org.dromara.northstar.data.jdbc.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class MessageSenderSettingsDO {

	@Id
	private String className;
	
	@Lob
	private String settingsData;
}
