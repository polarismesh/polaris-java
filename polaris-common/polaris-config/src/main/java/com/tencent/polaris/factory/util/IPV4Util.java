package com.tencent.polaris.factory.util;

/**
 * IPV4工具类
 *
 * @author starkwen
 * @date 2020/11/20 10:51 上午
 */
public class IPV4Util {

    // ip转数字
    public static int ipToInt(String ipAddress) {
        int result = 0;
        String[] ipAddressInArray = ipAddress.split("\\.");
        for (int i = 3; i >= 0; i--) {
            long ip = Long.parseLong(ipAddressInArray[3 - i]);
            result |= ip << (i * 8);
        }
        return result;
    }

    // 数字转ip
    public static String intToIp(int ip) {
        StringBuilder result = new StringBuilder(15);
        for (int i = 0; i < 4; i++) {
            result.insert(0, ip & 0xff);
            if (i < 3) {
                result.insert(0, '.');
            }
            ip = ip >> 8;
        }
        return result.toString();
    }

}
