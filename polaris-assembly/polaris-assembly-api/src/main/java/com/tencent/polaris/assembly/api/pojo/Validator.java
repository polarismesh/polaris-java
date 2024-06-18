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

package com.tencent.polaris.assembly.api.pojo;

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.util.CommonValidator;

public class Validator {

	/**
	 * 校验获取单个服务实例的请求
	 *
	 * @param request 请求对象
	 * @throws PolarisException 校验失败会抛出异常
	 */
	public static void validateGetOneInstanceRequest(GetOneInstanceRequest request) throws PolarisException {
		CommonValidator.validateNamespaceService(request.getNamespace(), request.getService());
	}

	/**
	 * 校验服务名和命名空间
	 *
	 * @param serviceKey 请求对象
	 * @throws PolarisException 校验失败会抛出异常
	 */
	public static void validateServiceKey(ServiceKey serviceKey) throws PolarisException {
		CommonValidator.validateNamespaceService(serviceKey.getNamespace(), serviceKey.getService());
	}

	/**
	 * 校验获取批量服务实例的请求
	 *
	 * @param request 请求对象
	 * @throws PolarisException 校验失败会抛出异常
	 */
	public static void validateGetReachableInstancesRequest(GetReachableInstancesRequest request) throws PolarisException {
		CommonValidator.validateNamespaceService(request.getNamespace(), request.getService());
	}

	/**
	 * 校验用户上报的调用结果
	 *
	 * @param serviceCallResult 调用结果
	 * @throws PolarisException 校验失败会抛出异常
	 */
	public static void validateServiceCallResult(ServiceCallResult serviceCallResult) throws PolarisException {
		CommonValidator.validateNamespaceService(serviceCallResult.getNamespace(), serviceCallResult.getService());
		if (null == serviceCallResult.getRetStatus()) {
			throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT, "retStatus can not be blank");
		}
		if (null != serviceCallResult.getDelay() && serviceCallResult.getDelay() < 0) {
			throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT, "delay can not be less than 0");
		}
		if (!RetStatus.RetReject.equals(serviceCallResult.getRetStatus())) {
			validateHostPort(serviceCallResult.getHost(), serviceCallResult.getPort());
		}
	}

	/**
	 * 校验端口信息
	 *
	 * @param port 端口类型
	 * @throws PolarisException 校验失败异常
	 */
	private static void validateHostPort(String host, Integer port) throws PolarisException {
		if (StringUtils.isBlank(host)) {
			throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT, "host can not be blank");
		}
		if (port == null) {
			throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT, "port can not be null");
		}
		if (port <= 0 || port >= CommonValidator.MAX_PORT) {
			throw new PolarisException(
					ErrorCode.API_INVALID_ARGUMENT, "port value should be in range (0, 65536).");
		}
	}


}
