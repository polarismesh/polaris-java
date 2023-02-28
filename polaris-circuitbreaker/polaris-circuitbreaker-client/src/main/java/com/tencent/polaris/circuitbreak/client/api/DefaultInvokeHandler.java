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

package com.tencent.polaris.circuitbreak.client.api;

import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.MethodResource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.ServiceResource;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.circuitbreak.api.InvokeHandler;
import com.tencent.polaris.circuitbreak.api.pojo.CheckResult;
import com.tencent.polaris.circuitbreak.api.pojo.InvokeContext;
import com.tencent.polaris.circuitbreak.api.pojo.ResultToErrorCode;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;

public class DefaultInvokeHandler implements InvokeHandler {

	private final CircuitBreakAPI circuitBreakAPI;

	public DefaultInvokeHandler(CircuitBreakAPI circuitBreakAPI) {
		this.circuitBreakAPI = circuitBreakAPI;
	}

	@Override
	public void acquirePermission(InvokeContext invokeContext) {
		CheckResult check = commonCheck(invokeContext.getRequestContext());
		if (check != null){
			throw new CallAbortedException(check.getRuleName(), check.getFallbackInfo());
		}
	}

	@Override
	public void onSuccess(InvokeContext invokeContext) {
		InvokeContext.RequestContext requestContext = invokeContext.getRequestContext();
		InvokeContext.ResponseContext responseContext = invokeContext.getResponseContext();
		long delay = responseContext.getDurationUnit().toMillis(responseContext.getDuration());
		ResultToErrorCode resultToErrorCode = requestContext.getResultToErrorCode();
		int code = 0;
		RetStatus retStatus = RetStatus.RetUnknown;
		if (null != resultToErrorCode) {
			code = resultToErrorCode.onSuccess(responseContext.getResult());
		}
		commonReport(requestContext, delay, code, retStatus);
	}

	@Override
	public void onError(InvokeContext invokeContext) {
		InvokeContext.RequestContext requestContext = invokeContext.getRequestContext();
		InvokeContext.ResponseContext responseContext = invokeContext.getResponseContext();
		long delay = responseContext.getDurationUnit().toMillis(responseContext.getDuration());
		ResultToErrorCode resultToErrorCode = requestContext.getResultToErrorCode();
		int code = -1;
		RetStatus retStatus = RetStatus.RetUnknown;
		if (null != resultToErrorCode) {
			code = resultToErrorCode.onError(responseContext.getError());
		}
		if (responseContext.getError() instanceof CallAbortedException) {
			retStatus = RetStatus.RetReject;
		}
		commonReport(requestContext, delay, code, retStatus);
	}

	private CheckResult commonCheck(InvokeContext.RequestContext requestContext) {
		// check service
		Resource svcResource = new ServiceResource(requestContext.getService(),
				requestContext.getSourceService());
		CheckResult check = circuitBreakAPI.check(svcResource);
		if (!check.isPass()) {
			return check;
		}
		// check method
		if (StringUtils.isNotBlank(requestContext.getMethod())) {
			Resource methodResource = new MethodResource(requestContext.getService(),
					requestContext.getMethod(), requestContext.getSourceService());
			check = circuitBreakAPI.check(methodResource);
			if (!check.isPass()) {
				return check;
			}
		}
		return null;
	}

	private void commonReport(InvokeContext.RequestContext requestContext, long delayMills, int code, RetStatus retStatus) {
		// report service
		Resource svcResource = new ServiceResource(requestContext.getService(),
				requestContext.getSourceService());
		ResourceStat resourceStat = new ResourceStat(svcResource, code, delayMills, retStatus);
		circuitBreakAPI.report(resourceStat);
		// report method
		if (StringUtils.isNotBlank(requestContext.getMethod())) {
			Resource methodResource = new MethodResource(requestContext.getService(),
					requestContext.getMethod(), requestContext.getSourceService());
			resourceStat = new ResourceStat(methodResource, code, delayMills, retStatus);
			circuitBreakAPI.report(resourceStat);
		}
	}

}
