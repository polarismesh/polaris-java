package com.tencent.polaris.configuration.client.util;

import com.google.common.base.Function;

/**
 * @author lepdou 2022-03-02
 */
public interface ConvertFunctions {

    Function<String, Integer> TO_INT_FUNCTION = Integer::parseInt;

    Function<String, Long> TO_LONG_FUNCTION = Long::parseLong;

    Function<String, Short> TO_SHORT_FUNCTION = Short::parseShort;

    Function<String, Float> TO_FLOAT_FUNCTION = Float::parseFloat;

    Function<String, Double> TO_DOUBLE_FUNCTION = Double::parseDouble;

    Function<String, Byte> TO_BYTE_FUNCTION = Byte::parseByte;

    Function<String, Boolean> TO_BOOLEAN_FUNCTION = Boolean::parseBoolean;

}
