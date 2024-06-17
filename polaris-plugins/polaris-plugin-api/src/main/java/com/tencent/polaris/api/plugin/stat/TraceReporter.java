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

package com.tencent.polaris.api.plugin.stat;

import java.util.Map;

import com.tencent.polaris.api.plugin.Plugin;

/**
 * 【扩展点接口】上报调用链
 *
 * @author andrewshan
 * @date 2024/6/2
 */
public interface TraceReporter extends Plugin {

	/**
	 * set the attributes in trace span
	 * @param attributes span attributes
	 */
	void setSpanAttributes(Map<String, String> attributes);

	/**
	 * set the attributes in baggage span
	 * @param attributes baggage attributes
	 */
	void setBaggageAttributes(Map<String, String> attributes);
}
