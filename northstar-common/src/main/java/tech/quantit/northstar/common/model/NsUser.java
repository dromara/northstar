package tech.quantit.northstar.common.model;

import java.io.Serializable;

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
public class NsUser implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 8553220725430453845L;

	private String userName;
	
	private String password;

}
