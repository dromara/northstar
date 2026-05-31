package org.dromara.northstar.data.jdbc.entity;

import com.alibaba.fastjson2.JSON;

import cn.hutool.core.lang.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name="ACCOUNTS", indexes = {
		@Index(name="idx_account_name", columnList = "accountName"),
})
public class AccountDO {

	@Id
	private String id;
	
	private String accountName;
	
	@Lob
	private String dataStr;
	
	public static AccountDO convertFrom(String accountName, org.dromara.northstar.common.model.core.Account account) {
		AccountDO do_ = new AccountDO();
		do_.setId(UUID.randomUUID().toString());
		do_.setAccountName(accountName);
		do_.setDataStr(JSON.toJSONString(account));
		return do_;
	}
	
	public org.dromara.northstar.common.model.core.Account convertTo() {
		return JSON.parseObject(dataStr, org.dromara.northstar.common.model.core.Account.class);
	}
	
}
