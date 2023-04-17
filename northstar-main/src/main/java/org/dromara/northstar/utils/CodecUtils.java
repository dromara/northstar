package org.dromara.northstar.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.apache.commons.io.FileUtils;

import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.symmetric.DES;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CodecUtils {

	private static final String KEY = "NORTHSTA";
	private static String salt = "";
	static {
		String homeDir = System.getProperty("user.home");
		File tempSalt = new File(homeDir, ".northstar-salt");
		try {
			if (tempSalt.exists()) {
				salt = FileUtils.readFileToString(tempSalt, StandardCharsets.UTF_8);

			} else {
				if (tempSalt.createNewFile()) {
					log.info("创建随机盐文件：{}", homeDir);
					Random r = new Random();
					salt = String.format("%08d", r.nextInt(100000000));
					FileUtils.write(tempSalt, salt, StandardCharsets.UTF_8);
				} else {
					log.warn("无法创建加密临时文件");
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		log.info("随机盐：{}", salt);
	}
	private static DES des = new DES(Mode.CBC, Padding.PKCS5Padding, KEY.getBytes(), salt.getBytes());

	public static String decrypt(String encodeStr) {
		return des.decryptStr(encodeStr);
	}

	public static String encrypt(String message) {
		return des.encryptBase64(message);
	}

}
