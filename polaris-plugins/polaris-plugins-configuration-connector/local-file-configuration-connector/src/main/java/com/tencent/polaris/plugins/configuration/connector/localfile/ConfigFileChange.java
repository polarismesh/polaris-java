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

package com.tencent.polaris.plugins.configuration.connector.localfile;

/**
 * @author rod.xu
 * @date 2022/9/28 10:29 上午
 */
public class ConfigFileChange {

	public ConfigFileChange(ChangeType changeType, String fileName) {
		this.fileName = fileName;
		this.changeType = changeType;
	}

	private String fileName;

	private ChangeType changeType;

	public enum ChangeType {

		/**
		 * create file
		 */
		CREATE,
		UPDATE,
		DELETE
	}

	public String getFileName() {
		return fileName;
	}

	public ChangeType getChangeType() {
		return changeType;
	}
}
