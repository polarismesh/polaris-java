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

package com.tencent.polaris.api.plugin.compose;

import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpHandler;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.HttpServerAware;
import com.tencent.polaris.api.plugin.Plugin;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;

public class MockHttpServerAware implements HttpServerAware, Plugin {

	private String host;

	private int port;

	private final Map<String, HttpHandler> handlers = new HashMap<>();

	private String name;

	private boolean portDrift;

	@Override
	public String getHost() {
		return host;
	}

	@Override
	public int getPort() {
		return port;
	}

	@Override
	public Map<String, HttpHandler> getHandlers() {
		return handlers;
	}

	@Override
	public boolean allowPortDrift() {
		return portDrift;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public PluginType getType() {
		return null;
	}

	@Override
	public void init(InitContext ctx) throws PolarisException {

	}

	@Override
	public void postContextInit(Extensions ctx) throws PolarisException {

	}

	@Override
	public void destroy() {

	}

	void setHost(String host) {
		this.host = host;
	}

	void setPort(int port) {
		this.port = port;
	}

	void setName(String name) {
		this.name = name;
	}

	void setPortDrift(boolean portDrift) {
		this.portDrift = portDrift;
	}
}
