/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.api.config.verify;


import com.tencent.polaris.api.config.consumer.LoadBalanceConfig;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;

import java.time.Duration;

/**
 * 默认值定义
 *
 * @author andrewshan
 * @date 2019/8/20
 */
public interface DefaultValues {

	/**
	 * 默认API调用的超时时间, 1s
	 */
	long DEFAULT_API_INVOKE_TIMEOUT_MS = Duration.parse("PT5S").toMillis();

	/**
	 * 默认API重试间隔, 500ms
	 */
	long DEFAULT_API_RETRY_INTERVAL_MS = 500;


	/**
	 * 默认API上报间隔, 30s
	 */
	long DEFAULT_API_REPORT_INTERVAL_MS = Duration.parse("PT30S").toMillis();

	/**
	 * 默认的服务超时淘汰时间，1H
	 */
	long DEFAULT_SERVICE_EXPIRE_TIME_MS = Duration.parse("PT1H").toMillis();

	/**
	 * 最小服务超时淘汰时间，5s
	 */
	long MIN_SERVICE_EXPIRE_TIME_MS = Duration.parse("PT5S").toMillis();

	/**
	 * 默认的服务刷新间隔, 2s
	 */
	long MIN_SERVICE_REFRESH_INTERVAL_MS = Duration.parse("PT2S").toMillis();

	/**
	 * 默认消息的超时时间
	 */
	long DEFAULT_SERVER_MSG_TIMEOUT_MS = 1000;

	/**
	 * 默认消息的超时时间
	 */
	long DEFAULT_SERVER_SERVICE_REFRESH_INTERVAL_MS = 60000;

	/**
	 * 默认SDK往Server连接超时时间间隔
	 */
	long DEFAULT_SERVER_CONNECT_TIMEOUT_MS = 50;

	/**
	 * 默认发送队列的buffer大小，支持的最大瞬时并发度，默认10W
	 */
	int DEFAULT_REQUEST_QUEUE_SIZE = 100000;

	/**
	 * 默认server的切换时间时间
	 */
	long DEFAULT_SERVER_SWITCH_INTERVAL_MS = Duration.parse("PT10M").toMillis();

	/**
	 * 默认空闲连接过期时间
	 */
	long DEFAULT_CONNECTION_IDLE_TIMEOUT_MS = Duration.parse("PT60S").toMillis();

	/**
	 * 默认缓存持久化目录
	 */
	String DEFAULT_CACHE_PERSIST_DIR = "./polaris/backup";

	String CONFIG_FILE_DEFAULT_CACHE_PERSIST_DIR = DEFAULT_CACHE_PERSIST_DIR + "/config";

	/**
	 * 是否打开熔断能力，默认true
	 */
	boolean DEFAULT_CIRCUIT_BREAKER_ENABLE = true;

	/**
	 * 是否打开健康探测能力，默认true
	 */
	boolean DEFAULT_OUTLIER_DETECT_ENABLE = true;

	/**
	 * 默认熔断节点检查周期
	 */
	long DEFAULT_CIRCUIT_BREAKER_CHECK_PERIOD_MS = 30 * 1000;

	/**
	 * 最小节点检查周期
	 */
	long MIN_CIRCUIT_BREAKER_CHECK_PERIOD_MS = 1000;

	/**
	 * 默认熔断周期，被熔断后多久变为半开
	 */
	long DEFAULT_SLEEP_WINDOW_MS = 30 * 1000;

	/**
	 * 最小熔断周期
	 */
	long MIN_SLEEP_WINDOW_MS = 1000;

	/**
	 * 默认恢复周期，半开后按多久的统计窗口进行恢复统计
	 */
	long DEFAULT_RECOVER_WINDOW_MS = 60 * 1000;

	/**
	 * 最小恢复周期
	 */
	long MIN_RECOVER_WINDOW_MS = 10 * 6000;

	/**
	 * 默认恢复统计的滑桶数
	 */
	int DEFAULT_RECOVER_NUM_BUCKETS = 10;

	/**
	 * 最小恢复统计的滑桶数
	 */
	int MIN_RECOVER_NUM_BUCKETS = 1;

	/**
	 * 半开状态后分配的探测请求数
	 */
	int DEFAULT_REQUEST_COUNT_AFTER_HALF_OPEN = 10;

	/**
	 * 半开状态后恢复的成功请求数
	 */
	int DEFAULT_SUCCESS_COUNT_AFTER_HALF_OPEN = 8;

	/**
	 * 默认的服务端连接器插件
	 */
	String DEFAULT_SERVERCONNECTOR = DefaultPlugins.SERVER_CONNECTOR_GRPC;

	/**
	 * 默认本地缓存策略
	 */
	String DEFAULT_LOCALCACHE = DefaultPlugins.LOCAL_REGISTRY_IN_MEMORY;

	/**
	 * 默认负载均衡器
	 */
	String DEFAULT_LOADBALANCER = LoadBalanceConfig.LOAD_BALANCE_WEIGHTED_RANDOM;

	/**
	 * 默认错误率熔断器
	 */
	String DEFAULT_CIRCUITBREAKER_ERRRATE = DefaultPlugins.CIRCUIT_BREAKER_ERROR_RATE;

	/**
	 * 默认持续错误熔断器
	 */
	String DEFAULT_CIRCUITBREAKER_ERRCOUNT = DefaultPlugins.CIRCUIT_BREAKER_ERROR_COUNT;

	/**
	 * 默认健康探测手段，tcp
	 */
	String DEFAULT_HEALTH_CHECKER_TCP = "tcp";

	/**
	 * 默认健康探测手段，udp
	 */
	String DEFAULT_HEALTH_CHECKER_UDP = "udp";

	/**
	 * 默认健康探测手段，http
	 */
	String DEFAULT_HEALTH_CHECKER_HTTP = "http";

	/**
	 * 默认权重值
	 */
	int DEFAULT_WEIGHT = 100;

	/**
	 * 默认负载均衡放开限制的故障节点上限阈值
	 */
	double DEFAULT_MAX_EJECT_PRECENT_THRESHOLD = 0.9;

	/**
	 * 默认最大重试次数，默认不重试
	 */
	int DEFAULT_MAX_RETRY_TIMES = 0;

	/**
	 * 默认缓存最大写重试次数
	 */
	int DEFAULT_PERSIST_MAX_WRITE_RETRY = 1;

	/**
	 * 默认缓存最大读重试次数
	 */
	int DEFAULT_PERSIST_MAX_READ_RETRY = 0;

	/**
	 * 默认缓存持久化时间间隔
	 */
	long DEFAULT_PERSIST_RETRY_INTERVAL_MS = 500;

	/**
	 * 默认健康探测周期
	 */
	long DEFAULT_OUTLIER_DETECT_INTERVAL_MS = Duration.parse("PT10S").toMillis();

	/**
	 * 最小定时任务轮询周期
	 */
	long MIN_TIMING_INTERVAL_MS = 100;

	String DEFAULT_SYSTEM_NAMESPACE = "Polaris";
	String DEFAULT_SYSTEM_REFRESH_INTERVAL = "10m";

	String DEFAULT_BUILTIN_DISCOVER = "polaris.builtin";

	/**
	 * 默认连接器协议
	 */
	String DEFAULT_DISCOVER_PROTOCOL = "grpc";

	/**
	 * 默认启用本地缓存机制
	 */
	boolean DEFAULT_PERSIST_ENABLE = true;

	String LOCAL_FILE_CONNECTOR_TYPE = "localFile";

	String PATTERN_CONFIG_FILE = "%s#%s#%s.yaml";

}
