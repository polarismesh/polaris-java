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

package com.tencent.polaris.test.common;

import java.util.Collection;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.plugin.Supplier;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.ValueContext;
import com.tencent.polaris.api.plugin.compose.ServerServiceInfo;

public class MockInitContext implements InitContext {

	private final Configuration configuration;

	public MockInitContext(Configuration configuration) {
		this.configuration = configuration;
	}

	@Override
	public Configuration getConfig() {
		return configuration;
	}

	@Override
	public Supplier getPlugins() {
		return null;
	}

	@Override
	public ValueContext getValueContext() {
		return null;
	}

	@Override
	public Collection<ServerServiceInfo> getServerServices() {
		return null;
	}
}
