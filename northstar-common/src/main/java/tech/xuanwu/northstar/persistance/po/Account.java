package tech.xuanwu.northstar.persistance.po;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;

import lombok.Data;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreField.AccountField;

@Data
public class Account implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -4485597896479804593L;

	@Id
	private String id;
	private String accountId;
	private String code;
	private String name;
	private String holder;
	private CurrencyEnum currency;
	private double preBalance;
	private double balance;
	private double available;
	private double commission;
	private double margin;
	private double closeProfit;
	private double positionProfit;
	private double deposit;
	private double withdraw;
	private String gatewayId;
	private Map<String, Position> positionMap = new HashMap<>();
	
	public static Account convertFrom(AccountField account) {
		Account po = new Account();
		BeanUtils.copyProperties(account.toBuilder(), po);
		return po;
	}
	
	public AccountField convertTo() {
		AccountField.Builder ab = AccountField.newBuilder();
		BeanUtils.copyProperties(this, ab);
		return ab.build();
	}
}
