package com.binance.connector.client.utils;

import com.binance.connector.client.exceptions.BinanceConnectorException;
import java.util.LinkedHashMap;

public final class ParameterChecker {

    private ParameterChecker() {
    }

    public static void checkParameter(LinkedHashMap<String, Object> parameters, String parameter, Class t) {
        checkRequiredParameter(parameters, parameter);
        checkParameterType(parameters.get(parameter), t, parameter);
    }

    public static void checkOrParameters(LinkedHashMap<String, Object> parameters, String parameter, String parameter2) {
        if (!parameters.containsKey(parameter) && (!parameters.containsKey(parameter2))) {
            throw new BinanceConnectorException(String.format("Either \"%s\" or \"%s\" is required!", parameter, parameter2));
        }
    }

    public static void checkRequiredParameter(LinkedHashMap<String, Object> parameters, String parameter) {
        if (!parameters.containsKey(parameter)) {
            throw new BinanceConnectorException(String.format("\"%s\" is a mandatory parameter!", parameter));
        }
    }

    public static void checkParameterType(Object parameter, Class t, String name) {
        if (!t.isInstance(parameter)) {
            throw new BinanceConnectorException(String.format("\"%s\" must be of %s type.", name, t));
        } else if (t == String.class && parameter.toString().trim().equals("")) {
            throw new BinanceConnectorException(String.format("\"%s\" must not be empty.", name));
        }
    }
}
