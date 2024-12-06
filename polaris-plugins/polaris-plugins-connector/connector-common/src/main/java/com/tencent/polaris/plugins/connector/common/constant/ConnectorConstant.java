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

package com.tencent.polaris.plugins.connector.common.constant;

import com.google.common.collect.Lists;

import java.util.List;

import static com.tencent.polaris.api.config.plugin.DefaultPlugins.*;

/**
 * @author Haotian Zhang
 */
public interface ConnectorConstant {

    String SERVER_CONNECTOR_TYPE = "SERVER_CONNECTOR_TYPE";

    List<String> ORDER_LIST = Lists.newArrayList(SERVER_CONNECTOR_GRPC, SERVER_CONNECTOR_CONSUL, SERVER_CONNECTOR_NACOS);
}
