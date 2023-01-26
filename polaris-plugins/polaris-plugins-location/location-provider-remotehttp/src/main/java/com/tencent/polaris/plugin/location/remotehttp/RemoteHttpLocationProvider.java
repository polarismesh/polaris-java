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

package com.tencent.polaris.plugin.location.remotehttp;

import com.tencent.polaris.specification.api.v1.model.ModelProto;
import java.io.IOException;

import com.google.protobuf.StringValue;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugin.location.base.BaseLocationProvider;
import org.slf4j.Logger;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class RemoteHttpLocationProvider extends BaseLocationProvider<BaseLocationProvider.GetOption> {

	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteHttpLocationProvider.class);

	private final OkHttpClient httpClient = new OkHttpClient();

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

	private String getResponse(final String url, String label)  {
		if (StringUtils.isEmpty(url)) {
			LOGGER.warn("[Location][Provider][RemoteHttp] get {} from remote url is empty", label);
			return "";
		}

		Request request = new Request.Builder()
				.get()
				.url(url)
				.build();

		Call call = httpClient.newCall(request);

		try {
			Response response = call.execute();
			byte[] ret = response.body().bytes();
			if (response.code() != 200) {
				LOGGER.error("[Location][Provider][RemoteHttp] get {} from remote {} fail: {}", label, url, new String(ret));
				return "";
			}
			return new String(ret);
		}
		catch (IOException e) {
			LOGGER.error("[Location][Provider][RemoteHttp] get {} from remote {} fail : {}", label, url, e.getMessage());
			return "";
		}
	}
}
