/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.plugin.location.remotehttp;

import java.io.BufferedReader;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.StringValue;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugin.location.base.BaseLocationProvider;
import org.slf4j.Logger;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class RemoteHttpLocationProvider extends BaseLocationProvider<BaseLocationProvider.GetOption> {

	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteHttpLocationProvider.class);

	public RemoteHttpLocationProvider() {
		super(GetOption.class);
	}

	@Override
	public ProviderType getProviderType() {
		return ProviderType.REMOTE_HTTP;
	}

	@Override
	public ModelProto.Location doGet(GetOption option) {
		String region = getResponse(option.getRegion(), "region");
		String zone = getResponse(option.getZone(), "zone");
		String campus = getResponse(option.getCampus(), "campus");

		if (StringUtils.isAllEmpty(region, zone, campus)) {
			return null;
		}

		LOGGER.info("[Location][Provider][RemoteHttp] get location from remote http : {}", option);

		return ModelProto.Location.newBuilder()
				.setRegion(StringValue.newBuilder().setValue(region).build())
				.setZone(StringValue.newBuilder().setValue(zone).build())
				.setCampus(StringValue.newBuilder().setValue(campus).build())
				.build();
	}

	private String getResponse(final String path, String label)  {
		if (StringUtils.isEmpty(path)) {
			LOGGER.warn("[Location][Provider][RemoteHttp] get {} from remote url is empty", label);
			return "";
		}

		HttpURLConnection conn = null;
		try {
			URL url = new java.net.URL(path);
			conn = (HttpURLConnection) url.openConnection();

			conn.setRequestMethod("GET");
			conn.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(2));// 连接超时
			conn.setReadTimeout((int) TimeUnit.SECONDS.toMillis(2));// 读取超时
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuffer buffer = new StringBuffer();
			String str;
			while((str = reader.readLine())!= null){
				buffer.append(str);
			}
			if (conn.getResponseCode() != 200) {
				LOGGER.error("[Location][Provider][RemoteHttp] get {} from remote {} fail: {}", label, url, buffer);
				return "";
			}
			return buffer.toString();
		}
		catch (IOException e) {
			LOGGER.error("[Location][Provider][RemoteHttp] get {} from remote {} fail : {}", label, path, e);
			return "";
		} finally {
			if (null != conn) {
				conn.disconnect();
			}
		}
	}
}
