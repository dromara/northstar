package org.dromara.northstar.data.jdbc.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name="MAIL_CONFIG")
public class MailConfigDescriptionDO {

	@Id
	private String id;
	@Lob
	private String dataStr;
}
