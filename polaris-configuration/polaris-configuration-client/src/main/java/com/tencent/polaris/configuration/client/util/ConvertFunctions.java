/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

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
