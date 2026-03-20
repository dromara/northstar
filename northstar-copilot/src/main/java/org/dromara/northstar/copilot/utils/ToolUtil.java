package org.dromara.northstar.copilot.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.dromara.northstar.copilot.annotation.FunctionCall;
import org.dromara.northstar.copilot.annotation.FunctionParameter;
import org.dromara.northstar.copilot.annotation.FunctionRequest;
import org.dromara.northstar.copilot.platform.openai.tool.Tool;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author cly
 * @Description TODO
 * @Date 2024/8/12 16:55
 */
import com.alibaba.fastjson2.JSON;

public class ToolUtil {

    private static final Logger log = LoggerFactory.getLogger(ToolUtil.class);
    static Reflections reflections = new Reflections(new ConfigurationBuilder()
            .setUrls(ClasspathHelper.forPackage(""))
            .setScanners(Scanners.TypesAnnotated));

    public static Map<String, Tool> toolEntityMap = new ConcurrentHashMap<>();
    public static Map<String, Class<?>> toolClassMap = new ConcurrentHashMap<>();
    public static Map<String, Class<?>> toolRequestMap = new ConcurrentHashMap<>();

    public static String invoke(String functionName, String argument) {
        long currentTimeMillis = System.currentTimeMillis();

        Class<?> functionClass = toolClassMap.get(functionName);
        Class<?> functionRequestClass = toolRequestMap.get(functionName);

        log.info("tool call function {}, argument {}", functionName, argument);

        try {
            // 获取调用函数
            Method apply = functionClass.getMethod("apply", functionRequestClass);

            // 解析参数
            Object arg = JSON.parseObject(argument, functionRequestClass);

            // 调用函数
            Object invoke = apply.invoke(functionClass.newInstance(), arg);


            String response = JSON.toJSONString(invoke);
            log.info("response {}, cost {} ms", response, System.currentTimeMillis() - currentTimeMillis);
            return response;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static List<Tool> getAllFunctionTools(List<String> functionList) {
        List<Tool> tools = new ArrayList<>();
        for (String functionName : functionList) {

            Tool tool = toolEntityMap.get(functionName);
            if(tool == null){
                tool = getToolEntity(functionName);
            }
            if(tool != null){
                toolEntityMap.put(functionName, tool);
                tools.add(tool);
            }


        }
        return !tools.isEmpty() ? tools : null;
    }


    public static Tool getToolEntity(String functionName){

        Tool.Function functionEntity = getFunctionEntity(functionName);
        if (functionEntity != null){
            Tool tool = new Tool();
            tool.setType("function");
            tool.setFunction(functionEntity);
            return tool;
        }

        return null;
    }


    public static Tool.Function getFunctionEntity(String functionName) {
        Set<Class<?>> functionSet = reflections.getTypesAnnotatedWith(FunctionCall.class);

        for (Class<?> functionClass : functionSet) {
            FunctionCall functionCall = functionClass.getAnnotation(FunctionCall.class);
            String currentFunctionName = functionCall.name();
            if(currentFunctionName.equals(functionName)) {
                Tool.Function function = new Tool.Function();
                function.setName(currentFunctionName);
                function.setDescription(functionCall.description());
                setFunctionParameters(function, functionClass);

                toolClassMap.put(functionName, functionClass);
                return function;
            }
        }
        return null;
    }

    private static void setFunctionParameters(Tool.Function function, Class<?> functionClass) {
        Class<?>[] classes = functionClass.getDeclaredClasses();
        Map<String, Tool.Function.Property> parameters = new HashMap<>();
        List<String> requiredParameters = new ArrayList<>();

        for (Class<?> clazz : classes) {
            FunctionRequest request = clazz.getAnnotation(FunctionRequest.class);
            if(request == null) continue;
            toolRequestMap.put(function.getName(), clazz);

            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                FunctionParameter parameter = field.getAnnotation(FunctionParameter.class);
                if(parameter == null) continue;
                Class<?> fieldType = field.getType();
                String jsonType = mapJavaTypeToJsonSchemaType(fieldType);
                Tool.Function.Property property = new Tool.Function.Property();
                property.setType(jsonType);
                property.setDescription(parameter.description());
                if(fieldType.isEnum()){
                    property.setEnumValues(getEnumValues(fieldType));
                }

                parameters.put(field.getName(), property);
                if(parameter.required()){
                    requiredParameters.add(field.getName());
                }
            }


        }

        Tool.Function.Parameter parameter = new Tool.Function.Parameter("object", parameters, requiredParameters);
        function.setParameters(parameter);
    }

    /**
     * 将Java类型映射到JSON Schema数据类型
     */
    private static String mapJavaTypeToJsonSchemaType(Class<?> fieldType) {
        if (fieldType.isEnum()) {
            return "string";
        } else if (fieldType.equals(String.class)) {
            return "string";
        } else if (fieldType.equals(int.class) || fieldType.equals(Integer.class) ||
                fieldType.equals(long.class) || fieldType.equals(Long.class) ||
                fieldType.equals(short.class) || fieldType.equals(Short.class) ||
                fieldType.equals(float.class) || fieldType.equals(Float.class) ||
                fieldType.equals(double.class) || fieldType.equals(Double.class)) {
            return "number";
        } else if (fieldType.equals(boolean.class) || fieldType.equals(Boolean.class)) {
            return "boolean";
        } else if (fieldType.isArray()) {
            return "array";
        } else if (Collection.class.isAssignableFrom(fieldType)) {
            return "array";
        } else if (Map.class.isAssignableFrom(fieldType)) {
            return "object";
        } else {
            return "object";
        }
    }

    /**
     * 获取枚举类型的所有可能值
     */
    private static List<String> getEnumValues(Class<?> enumType) {
        List<String> enumValues = new ArrayList<>();
        for (Object enumConstant : enumType.getEnumConstants()) {
            enumValues.add(enumConstant.toString());
        }
        return enumValues;
    }

}
