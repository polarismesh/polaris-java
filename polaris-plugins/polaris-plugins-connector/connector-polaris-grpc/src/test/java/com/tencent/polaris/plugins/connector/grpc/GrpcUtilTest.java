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
