package org.dromara.northstar.common.utils;

import java.lang.reflect.Field;

public class ReflectionUtil {
	
	private ReflectionUtil() {}

    /**
     * 设置对象的字段值。自动处理类型转换。
     *
     * @param obj       要修改的对象
     * @param fieldName 字段名称
     * @param value     新值
     * @throws NoSuchFieldException   如果字段不存在
     * @throws IllegalAccessException 如果字段不可访问
     */
    public static void setFieldValue(Object obj, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);  // 确保私有字段也可以访问

        Object convertedValue = convertValueToFieldType(value, field.getType());
        field.set(obj, convertedValue);
    }

    /**
     * 获取对象的字段值。
     *
     * @param obj       对象
     * @param fieldName 字段名称
     * @return 字段的值
     * @throws NoSuchFieldException   如果字段不存在
     * @throws IllegalAccessException 如果字段不可访问
     */
    public static Object getFieldValue(Object obj, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(fieldName);
        
        field.setAccessible(true);
        return field.get(obj);
    }

    /**
     * 转换值为目标字段类型。
     *
     * @param value     原始值
     * @param fieldType 字段类型
     * @return 转换后的值
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	private static Object convertValueToFieldType(Object value, Class<?> fieldType) {
        if (value == null) {
            return null;
        }
        if (fieldType.isAssignableFrom(value.getClass())) {
            return value;
        }

        // 对常见类型进行转换
        if (fieldType == int.class || fieldType == Integer.class) {
            return Integer.parseInt(value.toString());
        } else if (fieldType == long.class || fieldType == Long.class) {
            return Long.parseLong(value.toString());
        } else if (fieldType == double.class || fieldType == Double.class) {
            return Double.parseDouble(value.toString());
        } else if (fieldType == float.class || fieldType == Float.class) {
            return Float.parseFloat(value.toString());
        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
            return Boolean.parseBoolean(value.toString());
        } else if (fieldType == byte.class || fieldType == Byte.class) {
            return Byte.parseByte(value.toString());
        } else if (fieldType == short.class || fieldType == Short.class) {
            return Short.parseShort(value.toString());
        } else if (fieldType == char.class || fieldType == Character.class) {
            return value.toString().charAt(0);
        } else if(fieldType.isEnum()) {
        	return fieldType.cast(Enum.valueOf((Class<Enum>)fieldType, value.toString()));
        } else {
            throw new IllegalArgumentException("No conversion rule for " + fieldType);
        }
    }
}