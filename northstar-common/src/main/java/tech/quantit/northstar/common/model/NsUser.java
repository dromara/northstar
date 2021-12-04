package tech.quantit.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息
 * @author KevinHuangwl
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NsUser {

	private String userName;
	
	private String password;

}
