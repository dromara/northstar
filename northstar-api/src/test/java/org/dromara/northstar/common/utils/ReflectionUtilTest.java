package org.dromara.northstar.common.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ReflectionUtilTest {

	@Test
    void testSetFieldValue() throws NoSuchFieldException, IllegalAccessException {
        Mock mock = new Mock();
        
        // 测试设置字符串类型字段
        ReflectionUtil.setFieldValue(mock, "text", "new text");
        assertEquals("new text", mock.text);

        // 测试设置整型字段
        ReflectionUtil.setFieldValue(mock, "number", 10);
        assertEquals(10, mock.number);

        // 测试设置布尔类型字段
        ReflectionUtil.setFieldValue(mock, "flag", false);
        assertEquals(false, mock.flag);

        // 测试设置双精度类型字段
        ReflectionUtil.setFieldValue(mock, "value", 99.9D);
        assertEquals(99.9D, mock.value);

        // 测试设置长整型字段
        ReflectionUtil.setFieldValue(mock, "volume", 200000000L);
        assertEquals(200000000L, mock.volume);
    }

    @Test
    void testGetFieldValue() throws NoSuchFieldException, IllegalAccessException {
        Mock mock = new Mock();
        
        // 获取并验证每个字段类型的值
        assertEquals("default", ReflectionUtil.getFieldValue(mock, "text"));
        assertEquals(1, ReflectionUtil.getFieldValue(mock, "number"));
        assertEquals(true, ReflectionUtil.getFieldValue(mock, "flag"));
        assertEquals(55.5D, ReflectionUtil.getFieldValue(mock, "value"));
        assertEquals(100000000L, ReflectionUtil.getFieldValue(mock, "volume"));
    }
	
	
	static class Mock {
		
		private String text = "default";
		private int number = 1;
		private boolean flag = true;
		private double value = 55.5D;
		private long volume = 100000000L;
	}
}
