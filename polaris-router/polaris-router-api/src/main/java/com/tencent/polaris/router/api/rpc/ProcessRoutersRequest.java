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

package com.tencent.polaris.router.api.rpc;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.tencent.polaris.api.pojo.RouteArgument;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.SourceService;
import com.tencent.polaris.api.rpc.MetadataFailoverType;
import com.tencent.polaris.api.rpc.RequestBaseEntity;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.MapUtils;

/**
 * 路由处理请求
 */
public class ProcessRoutersRequest extends RequestBaseEntity {

	private SourceService sourceService;

	private RouterNamesGroup routers;

	private ServiceInstances dstInstances;

	private String method;

	//各个路由插件依赖的 metadata 参数
	private Map<String, Set<RouteArgument>> routerArgument;
	//元数据路由降级策略
	private MetadataFailoverType metadataFailoverType;

	/**
	 * 北极星内部治理规则执行时，会识别规则中的参数来源类别，如果发现规则中的参数来源指定为外部数据源时，会调用本接口进行获取
	 *
	 * 可以实现该接口，实现规则中的参数来源于配置中心、数据库、环境变量等等
	 */
	private Function<String, Optional<String>> externalParameterSupplier = s -> Optional.empty();

	public Function<String, Optional<String>> getExternalParameterSupplier() {
		return externalParameterSupplier;
	}

	public void setExternalParameterSupplier(Function<String, Optional<String>> externalParameterSupplier) {
		this.externalParameterSupplier = externalParameterSupplier;
	}

	public ServiceInfo getSourceService() {
		return sourceService;
	}

	public void setSourceService(ServiceInfo serviceInfo) {
		this.sourceService = new SourceService();
		this.sourceService.setService(serviceInfo.getService());
		this.sourceService.setNamespace(serviceInfo.getNamespace());

		Optional.ofNullable(serviceInfo.getMetadata()).orElse(new HashMap<>())
				.forEach((key, value) -> sourceService.appendArguments(RouteArgument.fromLabel(key, value)));

		buildRouterArgumentsBySourceService();
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public RouterNamesGroup getRouters() {
		return routers;
	}

	public void setRouters(RouterNamesGroup routers) {
		this.routers = routers;
	}

	public ServiceInstances getDstInstances() {
		return dstInstances;
	}

	public void setDstInstances(ServiceInstances dstInstances) {
		this.dstInstances = dstInstances;
	}

	public void putRouterArgument(String routerType, Set<RouteArgument> arguments) {
		if (CollectionUtils.isEmpty(arguments)) {
			return;
		}
		if (routerArgument == null) {
			routerArgument = new HashMap<>();
		}

		routerArgument.put(routerType, arguments);
	}

	public Set<RouteArgument> getRouterArguments(String routerType) {
		buildRouterArgumentsBySourceService();
		Set<RouteArgument> arguments = routerArgument.get(routerType);
		if (CollectionUtils.isEmpty(arguments)) {
			return Collections.emptySet();
		}

		return Collections.unmodifiableSet(arguments);
	}

	public Map<String, Set<RouteArgument>> getRouterArguments() {
		buildRouterArgumentsBySourceService();
		Map<String, Set<RouteArgument>> routerArgument = new HashMap<>(this.routerArgument);
		return Collections.unmodifiableMap(routerArgument);
	}

	public MetadataFailoverType getMetadataFailoverType() {
		return metadataFailoverType;
	}

	public void setMetadataFailoverType(MetadataFailoverType metadataFailoverType) {
		this.metadataFailoverType = metadataFailoverType;
	}

	private void buildRouterArgumentsBySourceService() {
		if (CollectionUtils.isEmpty(routerArgument)) {
			routerArgument = new HashMap<>();
		}
		if (Objects.isNull(sourceService)) {
			return;
		}
		Set<RouteArgument> arguments = routerArgument.computeIfAbsent("ruleRouter", k -> new HashSet<>());
		arguments.addAll(sourceService.getArguments());
	}

	@Deprecated
	public void putRouterMetadata(String routerType, Map<String, String> metadata) {
		if (MapUtils.isEmpty(metadata)) {
			return;
		}
		if (routerArgument == null) {
			routerArgument = new HashMap<>();
		}

		Set<RouteArgument> arguments = new HashSet<>();
		metadata.forEach((key, value) -> arguments.add(RouteArgument.fromLabel(key, value)));

		routerArgument.put(routerType, arguments);
	}

	@Deprecated
	public void addRouterMetadata(String routerType, Map<String, String> metadata) {
		if (MapUtils.isEmpty(metadata)) {
			return;
		}

		if (routerArgument == null) {
			routerArgument = new HashMap<>();
		}

		Set<RouteArgument> arguments = routerArgument.computeIfAbsent(routerType, k -> new HashSet<>());
		metadata.forEach((key, value) -> arguments.add(RouteArgument.fromLabel(key, value)));
	}

	@Deprecated
	public Map<String, String> getRouterMetadata(String routerType) {
		buildRouterArgumentsBySourceService();
		Set<RouteArgument> arguments = routerArgument.get(routerType);
		if (CollectionUtils.isEmpty(arguments)) {
			return Collections.emptyMap();
		}

		Map<String, String> metadata = new HashMap<>();
		arguments.forEach(argument -> argument.toLabel(metadata));

		return Collections.unmodifiableMap(metadata);
	}

	@Deprecated
	public Map<String, Map<String, String>> getRouterMetadata() {
		buildRouterArgumentsBySourceService();

		Map<String, Map<String, String>> ret = new HashMap<>();

		routerArgument.forEach((routerType, arguments) -> {
			Map<String, String> entry = ret.computeIfAbsent(routerType, k -> new HashMap<>());
			arguments.forEach(argument -> argument.toLabel(entry));
		});

		return ret;
	}

	public static class RouterNamesGroup {

		private List<String> beforeRouters;

		private List<String> coreRouters;

		private List<String> afterRouters;

		public List<String> getBeforeRouters() {
			return beforeRouters;
		}

		public void setBeforeRouters(List<String> beforeRouters) {
			this.beforeRouters = beforeRouters;
		}

		public List<String> getCoreRouters() {
			return coreRouters;
		}

		public void setCoreRouters(List<String> coreRouters) {
			this.coreRouters = coreRouters;
		}

		public List<String> getAfterRouters() {
			return afterRouters;
		}

		public void setAfterRouters(List<String> afterRouters) {
			this.afterRouters = afterRouters;
		}
	}
}
