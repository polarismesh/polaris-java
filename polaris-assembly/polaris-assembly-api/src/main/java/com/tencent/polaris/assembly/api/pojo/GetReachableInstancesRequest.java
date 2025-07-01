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

package com.tencent.polaris.assembly.api.pojo;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.tencent.polaris.api.pojo.RouteArgument;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.SourceService;
import com.tencent.polaris.api.rpc.MetadataFailoverType;
import com.tencent.polaris.api.rpc.RequestBaseEntity;

public class GetReachableInstancesRequest extends RequestBaseEntity {

	/**
	 * 服务元数据信息，用于服务路由过滤
	 */
	private Map<String, String> metadata;

	/**
	 * 主调方服务信息
	 */
	private SourceService sourceService;

	/**
	 * 是否返回熔断实例，默认否
	 */
	private boolean includeCircuitBreak;

	/**
	 * 是否返回不健康的服务实例，默认否
	 */
	private boolean includeUnhealthy;

	/**
	 * 金丝雀集群
	 */
	private String canary;

	/**
	 * 接口参数
	 */
	private String method;

	/**
	 * 可选, metadata失败降级策略
	 */
	private MetadataFailoverType metadataFailoverType;

	public boolean isIncludeCircuitBreak() {
		return includeCircuitBreak;
	}

	public void setIncludeCircuitBreak(boolean includeCircuitBreak) {
		this.includeCircuitBreak = includeCircuitBreak;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}

	public boolean isIncludeUnhealthy() {
		return includeUnhealthy;
	}

	public void setIncludeUnhealthy(boolean includeUnhealthy) {
		this.includeUnhealthy = includeUnhealthy;
	}


	public SourceService getServiceInfo() {
		return sourceService;
	}

	public void setServiceInfo(ServiceInfo serviceInfo) {
		if (serviceInfo instanceof SourceService) {
			this.sourceService = (SourceService) serviceInfo;
		} else {
			SourceService sourceService = new SourceService();
			sourceService.setNamespace(serviceInfo.getNamespace());
			sourceService.setService(serviceInfo.getService());
			Set<RouteArgument> argumentSet = new HashSet<>();
			Optional.ofNullable(serviceInfo.getMetadata()).orElse(Collections.emptyMap())
					.forEach((key, value) -> argumentSet.add(RouteArgument.fromLabel(key, value)));
			sourceService.setArguments(argumentSet);
			this.sourceService = sourceService;
		}
	}

	public String getCanary() {
		return canary;
	}

	public void setCanary(String canary) {
		this.canary = canary;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public MetadataFailoverType getMetadataFailoverType() {
		return metadataFailoverType;
	}

	public void setMetadataFailoverType(MetadataFailoverType metadataFailoverType) {
		this.metadataFailoverType = metadataFailoverType;
	}

	@Override
	@SuppressWarnings("checkstyle:all")
	public String toString() {
		return "GetInstancesRequest{" +
				"metadata=" + metadata +
				", sourceService=" + sourceService +
				", includeCircuitBreak=" + includeCircuitBreak +
				", includeUnhealthy=" + includeUnhealthy +
				", canary='" + canary + '\'' +
				", method='" + method + '\'' +
				", metadataFailoverType=" + metadataFailoverType +
				"} " + super.toString();
	}
}
