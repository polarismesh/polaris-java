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

package com.tencent.polaris.api.utils;

import com.tencent.polaris.logging.LoggerFactory;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import org.slf4j.Logger;

public class ConversionUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ConversionUtils.class);

    private static final String HEX_FLAG = "0x";

    private static final String OCT_FLAG = "0o";

    private static final String DEC_FLAG = "0d";

    private static final String BIN_FLAG = "0b";

    public static byte[] anyStringToByte(String value) {
        if (value.startsWith(HEX_FLAG)) {
            return hexStringToByte(value.substring(2));
        } else if (value.startsWith(OCT_FLAG)) {
            return octStringToByte(value.substring(2));
        } else if (value.startsWith(DEC_FLAG)) {
            return decStringToByte(value.substring(2));
        } else if (value.startsWith(BIN_FLAG)) {
            return binaryStringToByte(value.substring(2));
        }
        return value.getBytes();
    }

    public static byte[] hexStringToByte(String hex) {
        try {
            int i = Integer.parseInt(hex, 16);
            return bigIntToByteArray(i);
        } catch (Exception e) {
            LOG.error("fail to convert hex to byte", e);
            return null;
        }
    }

    public static byte[] decStringToByte(String hex) {
        try {
            int i = Integer.parseInt(hex, 10);
            return bigIntToByteArray(i);
        } catch (Exception e) {
            LOG.error("fail to convert hex to byte", e);
            return null;
        }
    }

    public static byte[] octStringToByte(String hex) {
        try {
            int i = Integer.parseInt(hex, 8);
            return bigIntToByteArray(i);
        } catch (Exception e) {
            LOG.error("fail to convert hex to byte", e);
            return null;
        }
    }

    public static byte[] binaryStringToByte(String hex) {
        try {
            int i = Integer.parseInt(hex, 2);
            return bigIntToByteArray(i);
        } catch (Exception e) {
            LOG.error("fail to convert hex to byte", e);
            return null;
        }
    }

    public static byte[] bigIntToByteArray(int i) {
        try {
            BigInteger bigInt = BigInteger.valueOf(i);
            return bigInt.toByteArray();
        } catch (Exception e) {
            LOG.error("fail to convert hex to byte", e);
            return null;
        }
    }

    private static Integer byteArrayToInt(byte[] intBytes) {
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(intBytes);
            return byteBuffer.getInt();
        } catch (Exception e) {
            LOG.error("fail to convert hex to byte", e);
            return null;
        }
    }

    public static String byteArrayToHexString(byte[] array) {
        Integer integer = byteArrayToInt(array);
        if (null == integer) {
            return null;
        }
        return Integer.toHexString(integer);
    }
    
}
