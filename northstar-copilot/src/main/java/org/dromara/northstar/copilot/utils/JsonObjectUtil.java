package org.dromara.northstar.copilot.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.Map;
import java.util.Set;

/**
 * @Author cly
 * @Description 用于JSON字符串驼峰转换的工具类
 * @Date 2024/8/30 23:12
 */
public class JsonObjectUtil {

    // 将 JSON 字符串的字段转换为大写驼峰形式
    public static String toCamelCaseWithUppercaseJson(String json) {
        return convertJsonString(json, true);
    }

    // 将 JSON 字符串的字段转换为下划线形式
    public static String toSnakeCaseJson(String json) {
        return convertJsonString(json, false);
    }

    // 根据参数 isCamelCase 决定转换为驼峰命名还是下划线命名
    private static String convertJsonString(String json, boolean isCamelCase) {
        JSONObject jsonObject = JSON.parseObject(json);
        JSONObject newJsonObject = processJsonObject(jsonObject, isCamelCase);
        return newJsonObject.toJSONString();
    }

    private static JSONObject processJsonObject(JSONObject jsonObject, boolean isCamelCase) {
        JSONObject newJsonObject = new JSONObject();
        Set<Map.Entry<String, Object>> entries = jsonObject.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            String newKey = isCamelCase ? toCamelCaseWithUppercase(entry.getKey()) : toSnakeCase(entry.getKey());
            Object value = entry.getValue();

            if (value instanceof JSONObject) {
                value = processJsonObject((JSONObject) value, isCamelCase);
            } else if (value instanceof JSONArray) {
                value = processJsonArray((JSONArray) value, isCamelCase);
            }

            newJsonObject.put(newKey, value);
        }
        return newJsonObject;
    }

    private static JSONArray processJsonArray(JSONArray jsonArray, boolean isCamelCase) {
        JSONArray newArray = new JSONArray();
        for (Object element : jsonArray) {
            if (element instanceof JSONObject) {
                newArray.add(processJsonObject((JSONObject) element, isCamelCase));
            } else if (element instanceof JSONArray) {
                newArray.add(processJsonArray((JSONArray) element, isCamelCase));
            } else {
                newArray.add(element);
            }
        }
        return newArray;
    }

    private static String toCamelCaseWithUppercase(String key) {
        String[] parts = key.split("_");
        StringBuilder camelCaseKey = new StringBuilder();
        for (String part : parts) {
            if (part.length() > 0) {
                camelCaseKey.append(part.substring(0, 1).toUpperCase())
                        .append(part.substring(1).toLowerCase());
            }
        }
        return camelCaseKey.toString();
    }

    private static String toSnakeCase(String key) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append("_");
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

}
