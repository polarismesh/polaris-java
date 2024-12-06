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

package com.tencent.polaris.plugins.connector.grpc;

import com.google.protobuf.UInt32Value;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * Test for{@link GrpcUtil}.
 *
 * @author Haotian Zhang
 */
public class GrpcUtilTest {

    @Test
    public void testCheckResponse() {
        ResponseProto.Response.Builder responseBuilder = ResponseProto.Response.newBuilder();

        // test 200000
        responseBuilder.setCode(UInt32Value.of(ServerCodes.EXECUTE_SUCCESS));
        try {
            GrpcUtil.checkResponse(responseBuilder.build());
        } catch (Exception e) {
            fail();
        }

        // test 400201
        responseBuilder.setCode(UInt32Value.of(ServerCodes.EXISTED_RESOURCE));
        try {
            GrpcUtil.checkResponse(responseBuilder.build());
        } catch (Exception e) {
            fail();
        }

        // test 200002
        responseBuilder.setCode(UInt32Value.of(ServerCodes.NO_NEED_UPDATE));
        try {
            GrpcUtil.checkResponse(responseBuilder.build());
        } catch (Exception e) {
            fail();
        }

        // test 400202
        responseBuilder.setCode(UInt32Value.of(ServerCodes.NOT_FOUND_RESOURCE));
        try {
            GrpcUtil.checkResponse(responseBuilder.build());
            fail();
        } catch (Exception e) {

        }
    }
}
