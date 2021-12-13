package com.tencent.polaris.ratelimit.client.pb;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.38.0)",
    comments = "Source: grpcapi_v2_metric.proto")
public final class RateLimitGRPCV2Grpc {

  private RateLimitGRPCV2Grpc() {}

  public static final String SERVICE_NAME = "polaris.metric.v2.RateLimitGRPCV2";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitRequest,
      com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitResponse> getServiceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Service",
      requestType = com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitRequest.class,
      responseType = com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitRequest,
      com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitResponse> getServiceMethod() {
    io.grpc.MethodDescriptor<com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitRequest, com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitResponse> getServiceMethod;
    if ((getServiceMethod = RateLimitGRPCV2Grpc.getServiceMethod) == null) {
      synchronized (RateLimitGRPCV2Grpc.class) {
        if ((getServiceMethod = RateLimitGRPCV2Grpc.getServiceMethod) == null) {
          RateLimitGRPCV2Grpc.getServiceMethod = getServiceMethod =
              io.grpc.MethodDescriptor.<com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitRequest, com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Service"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RateLimitGRPCV2MethodDescriptorSupplier("Service"))
              .build();
        }
      }
    }
    return getServiceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.tencent.polaris.ratelimit.client.pb.RatelimitV2.TimeAdjustRequest,
      com.tencent.polaris.ratelimit.client.pb.RatelimitV2.TimeAdjustResponse> getTimeAdjustMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "TimeAdjust",
      requestType = com.tencent.polaris.ratelimit.client.pb.RatelimitV2.TimeAdjustRequest.class,
      responseType = com.tencent.polaris.ratelimit.client.pb.RatelimitV2.TimeAdjustResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.tencent.polaris.ratelimit.client.pb.RatelimitV2.TimeAdjustRequest,
      com.tencent.polaris.ratelimit.client.pb.RatelimitV2.TimeAdjustResponse> getTimeAdjustMethod() {
    io.grpc.MethodDescriptor<com.tencent.polaris.ratelimit.client.pb.RatelimitV2.TimeAdjustRequest, com.tencent.polaris.ratelimit.client.pb.RatelimitV2.TimeAdjustResponse> getTimeAdjustMethod;
    if ((getTimeAdjustMethod = RateLimitGRPCV2Grpc.getTimeAdjustMethod) == null) {
      synchronized (RateLimitGRPCV2Grpc.class) {
        if ((getTimeAdjustMethod = RateLimitGRPCV2Grpc.getTimeAdjustMethod) == null) {
          RateLimitGRPCV2Grpc.getTimeAdjustMethod = getTimeAdjustMethod =
              io.grpc.MethodDescriptor.<com.tencent.polaris.ratelimit.client.pb.RatelimitV2.TimeAdjustRequest, com.tencent.polaris.ratelimit.client.pb.RatelimitV2.TimeAdjustResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "TimeAdjust"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.tencent.polaris.ratelimit.client.pb.RatelimitV2.TimeAdjustRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.tencent.polaris.ratelimit.client.pb.RatelimitV2.TimeAdjustResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RateLimitGRPCV2MethodDescriptorSupplier("TimeAdjust"))
              .build();
        }
      }
    }
    return getTimeAdjustMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static RateLimitGRPCV2Stub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RateLimitGRPCV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RateLimitGRPCV2Stub>() {
        @java.lang.Override
        public RateLimitGRPCV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RateLimitGRPCV2Stub(channel, callOptions);
        }
      };
    return RateLimitGRPCV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static RateLimitGRPCV2BlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RateLimitGRPCV2BlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RateLimitGRPCV2BlockingStub>() {
        @java.lang.Override
        public RateLimitGRPCV2BlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RateLimitGRPCV2BlockingStub(channel, callOptions);
        }
      };
    return RateLimitGRPCV2BlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static RateLimitGRPCV2FutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RateLimitGRPCV2FutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RateLimitGRPCV2FutureStub>() {
        @java.lang.Override
        public RateLimitGRPCV2FutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RateLimitGRPCV2FutureStub(channel, callOptions);
        }
      };
    return RateLimitGRPCV2FutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class RateLimitGRPCV2ImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * 限流接口
     * </pre>
     */
    public io.grpc.stub.StreamObserver<com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitRequest> service(
        io.grpc.stub.StreamObserver<com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitResponse> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getServiceMethod(), responseObserver);
    }

    /**
     * <pre>
     *时间对齐接口
     * </pre>
     */
    public void timeAdjust(com.tencent.polaris.ratelimit.client.pb.RatelimitV2.TimeAdjustRequest request,
        io.grpc.stub.StreamObserver<com.tencent.polaris.ratelimit.client.pb.RatelimitV2.TimeAdjustResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getTimeAdjustMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getServiceMethod(),
            io.grpc.stub.ServerCalls.asyncBidiStreamingCall(
              new MethodHandlers<
                com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitRequest,
                com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitResponse>(
                  this, METHODID_SERVICE)))
          .addMethod(
            getTimeAdjustMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.tencent.polaris.ratelimit.client.pb.RatelimitV2.TimeAdjustRequest,
                com.tencent.polaris.ratelimit.client.pb.RatelimitV2.TimeAdjustResponse>(
                  this, METHODID_TIME_ADJUST)))
          .build();
    }
  }

  /**
   */
  public static final class RateLimitGRPCV2Stub extends io.grpc.stub.AbstractAsyncStub<RateLimitGRPCV2Stub> {
    private RateLimitGRPCV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RateLimitGRPCV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RateLimitGRPCV2Stub(channel, callOptions);
    }

    /**
     * <pre>
     * 限流接口
     * </pre>
     */
    public io.grpc.stub.StreamObserver<com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitRequest> service(
        io.grpc.stub.StreamObserver<com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitResponse> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncBidiStreamingCall(
          getChannel().newCall(getServiceMethod(), getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     *时间对齐接口
     * </pre>
     */
    public void timeAdjust(com.tencent.polaris.ratelimit.client.pb.RatelimitV2.TimeAdjustRequest request,
        io.grpc.stub.StreamObserver<com.tencent.polaris.ratelimit.client.pb.RatelimitV2.TimeAdjustResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getTimeAdjustMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class RateLimitGRPCV2BlockingStub extends io.grpc.stub.AbstractBlockingStub<RateLimitGRPCV2BlockingStub> {
    private RateLimitGRPCV2BlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RateLimitGRPCV2BlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RateLimitGRPCV2BlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     *时间对齐接口
     * </pre>
     */
    public com.tencent.polaris.ratelimit.client.pb.RatelimitV2.TimeAdjustResponse timeAdjust(com.tencent.polaris.ratelimit.client.pb.RatelimitV2.TimeAdjustRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getTimeAdjustMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class RateLimitGRPCV2FutureStub extends io.grpc.stub.AbstractFutureStub<RateLimitGRPCV2FutureStub> {
    private RateLimitGRPCV2FutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RateLimitGRPCV2FutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RateLimitGRPCV2FutureStub(channel, callOptions);
    }

    /**
     * <pre>
     *时间对齐接口
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.tencent.polaris.ratelimit.client.pb.RatelimitV2.TimeAdjustResponse> timeAdjust(
        com.tencent.polaris.ratelimit.client.pb.RatelimitV2.TimeAdjustRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getTimeAdjustMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_TIME_ADJUST = 0;
  private static final int METHODID_SERVICE = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final RateLimitGRPCV2ImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(RateLimitGRPCV2ImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_TIME_ADJUST:
          serviceImpl.timeAdjust((com.tencent.polaris.ratelimit.client.pb.RatelimitV2.TimeAdjustRequest) request,
              (io.grpc.stub.StreamObserver<com.tencent.polaris.ratelimit.client.pb.RatelimitV2.TimeAdjustResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SERVICE:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.service(
              (io.grpc.stub.StreamObserver<com.tencent.polaris.ratelimit.client.pb.RatelimitV2.RateLimitResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class RateLimitGRPCV2BaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    RateLimitGRPCV2BaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.tencent.polaris.ratelimit.client.pb.GrpcapiV2Metric.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("RateLimitGRPCV2");
    }
  }

  private static final class RateLimitGRPCV2FileDescriptorSupplier
      extends RateLimitGRPCV2BaseDescriptorSupplier {
    RateLimitGRPCV2FileDescriptorSupplier() {}
  }

  private static final class RateLimitGRPCV2MethodDescriptorSupplier
      extends RateLimitGRPCV2BaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    RateLimitGRPCV2MethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (RateLimitGRPCV2Grpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new RateLimitGRPCV2FileDescriptorSupplier())
              .addMethod(getServiceMethod())
              .addMethod(getTimeAdjustMethod())
              .build();
        }
      }
    }
    return result;
  }
}
