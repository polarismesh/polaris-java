package com.tencent.polaris.plugins.connector.consul.service.authority.entity;

public class TsfAuthConstant {

    /**
     * 鉴权类型（微服务级别）
     *
     * @author hongweizhu
     */
    public static class TYPE {
        /**
         * 黑名单模式
         */
        public static final String BLACK_LIST = "B";
        /**
         * 白名单模式
         */
        public static final String WHITE_LIST = "W";
        /**
         * 不启用
         */
        public static final String DISABLED = "D";
    }
}
