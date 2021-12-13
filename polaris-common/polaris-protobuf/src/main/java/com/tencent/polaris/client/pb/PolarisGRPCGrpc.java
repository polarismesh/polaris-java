package com.tencent.polaris.client.pb;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.38.0)",
    comments = "Source: grpcapi_discovery.proto")
public final class PolarisGRPCGrpc {

  private PolarisGRPCGrpc() {}

  public static final String SERVICE_NAME = "v1.PolarisGRPC";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.tencent.polaris.client.pb.ClientProto.Client,
      com.tencent.polaris.client.pb.ResponseProto.Response> getReportClientMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ReportClient",
      requestType = com.tencent.polaris.client.pb.ClientProto.Client.class,
      responseType = com.tencent.polaris.client.pb.ResponseProto.Response.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.tencent.polaris.client.pb.ClientProto.Client,
      com.tencent.polaris.client.pb.ResponseProto.Response> getReportClientMethod() {
    io.grpc.MethodDescriptor<com.tencent.polaris.client.pb.ClientProto.Client, com.tencent.polaris.client.pb.ResponseProto.Response> getReportClientMethod;
    if ((getReportClientMethod = PolarisGRPCGrpc.getReportClientMethod) == null) {
      synchronized (PolarisGRPCGrpc.class) {
        if ((getReportClientMethod = PolarisGRPCGrpc.getReportClientMethod) == null) {
          PolarisGRPCGrpc.getReportClientMethod = getReportClientMethod =
              io.grpc.MethodDescriptor.<com.tencent.polaris.client.pb.ClientProto.Client, com.tencent.polaris.client.pb.ResponseProto.Response>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ReportClient"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.tencent.polaris.client.pb.ClientProto.Client.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.tencent.polaris.client.pb.ResponseProto.Response.getDefaultInstance()))
              .setSchemaDescriptor(new PolarisGRPCMethodDescriptorSupplier("ReportClient"))
              .build();
        }
      }
    }
    return getReportClientMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.tencent.polaris.client.pb.ServiceProto.Instance,
      com.tencent.polaris.client.pb.ResponseProto.Response> getRegisterInstanceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RegisterInstance",
      requestType = com.tencent.polaris.client.pb.ServiceProto.Instance.class,
      responseType = com.tencent.polaris.client.pb.ResponseProto.Response.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.tencent.polaris.client.pb.ServiceProto.Instance,
      com.tencent.polaris.client.pb.ResponseProto.Response> getRegisterInstanceMethod() {
    io.grpc.MethodDescriptor<com.tencent.polaris.client.pb.ServiceProto.Instance, com.tencent.polaris.client.pb.ResponseProto.Response> getRegisterInstanceMethod;
    if ((getRegisterInstanceMethod = PolarisGRPCGrpc.getRegisterInstanceMethod) == null) {
      synchronized (PolarisGRPCGrpc.class) {
        if ((getRegisterInstanceMethod = PolarisGRPCGrpc.getRegisterInstanceMethod) == null) {
          PolarisGRPCGrpc.getRegisterInstanceMethod = getRegisterInstanceMethod =
              io.grpc.MethodDescriptor.<com.tencent.polaris.client.pb.ServiceProto.Instance, com.tencent.polaris.client.pb.ResponseProto.Response>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RegisterInstance"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.tencent.polaris.client.pb.ServiceProto.Instance.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.tencent.polaris.client.pb.ResponseProto.Response.getDefaultInstance()))
              .setSchemaDescriptor(new PolarisGRPCMethodDescriptorSupplier("RegisterInstance"))
              .build();
        }
      }
    }
    return getRegisterInstanceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.tencent.polaris.client.pb.ServiceProto.Instance,
      com.tencent.polaris.client.pb.ResponseProto.Response> getDeregisterInstanceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeregisterInstance",
      requestType = com.tencent.polaris.client.pb.ServiceProto.Instance.class,
      responseType = com.tencent.polaris.client.pb.ResponseProto.Response.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.tencent.polaris.client.pb.ServiceProto.Instance,
      com.tencent.polaris.client.pb.ResponseProto.Response> getDeregisterInstanceMethod() {
    io.grpc.MethodDescriptor<com.tencent.polaris.client.pb.ServiceProto.Instance, com.tencent.polaris.client.pb.ResponseProto.Response> getDeregisterInstanceMethod;
    if ((getDeregisterInstanceMethod = PolarisGRPCGrpc.getDeregisterInstanceMethod) == null) {
      synchronized (PolarisGRPCGrpc.class) {
        if ((getDeregisterInstanceMethod = PolarisGRPCGrpc.getDeregisterInstanceMethod) == null) {
          PolarisGRPCGrpc.getDeregisterInstanceMethod = getDeregisterInstanceMethod =
              io.grpc.MethodDescriptor.<com.tencent.polaris.client.pb.ServiceProto.Instance, com.tencent.polaris.client.pb.ResponseProto.Response>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeregisterInstance"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.tencent.polaris.client.pb.ServiceProto.Instance.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.tencent.polaris.client.pb.ResponseProto.Response.getDefaultInstance()))
              .setSchemaDescriptor(new PolarisGRPCMethodDescriptorSupplier("DeregisterInstance"))
              .build();
        }
      }
    }
    return getDeregisterInstanceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.tencent.polaris.client.pb.RequestProto.DiscoverRequest,
      com.tencent.polaris.client.pb.ResponseProto.DiscoverResponse> getDiscoverMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Discover",
      requestType = com.tencent.polaris.client.pb.RequestProto.DiscoverRequest.class,
      responseType = com.tencent.polaris.client.pb.ResponseProto.DiscoverResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<com.tencent.polaris.client.pb.RequestProto.DiscoverRequest,
      com.tencent.polaris.client.pb.ResponseProto.DiscoverResponse> getDiscoverMethod() {
    io.grpc.MethodDescriptor<com.tencent.polaris.client.pb.RequestProto.DiscoverRequest, com.tencent.polaris.client.pb.ResponseProto.DiscoverResponse> getDiscoverMethod;
    if ((getDiscoverMethod = PolarisGRPCGrpc.getDiscoverMethod) == null) {
      synchronized (PolarisGRPCGrpc.class) {
        if ((getDiscoverMethod = PolarisGRPCGrpc.getDiscoverMethod) == null) {
          PolarisGRPCGrpc.getDiscoverMethod = getDiscoverMethod =
              io.grpc.MethodDescriptor.<com.tencent.polaris.client.pb.RequestProto.DiscoverRequest, com.tencent.polaris.client.pb.ResponseProto.DiscoverResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Discover"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.tencent.polaris.client.pb.RequestProto.DiscoverRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.tencent.polaris.client.pb.ResponseProto.DiscoverResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PolarisGRPCMethodDescriptorSupplier("Discover"))
              .build();
        }
      }
    }
    return getDiscoverMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.tencent.polaris.client.pb.ServiceProto.Instance,
      com.tencent.polaris.client.pb.ResponseProto.Response> getHeartbeatMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Heartbeat",
      requestType = com.tencent.polaris.client.pb.ServiceProto.Instance.class,
      responseType = com.tencent.polaris.client.pb.ResponseProto.Response.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.tencent.polaris.client.pb.ServiceProto.Instance,
      com.tencent.polaris.client.pb.ResponseProto.Response> getHeartbeatMethod() {
    io.grpc.MethodDescriptor<com.tencent.polaris.client.pb.ServiceProto.Instance, com.tencent.polaris.client.pb.ResponseProto.Response> getHeartbeatMethod;
    if ((getHeartbeatMethod = PolarisGRPCGrpc.getHeartbeatMethod) == null) {
      synchronized (PolarisGRPCGrpc.class) {
        if ((getHeartbeatMethod = PolarisGRPCGrpc.getHeartbeatMethod) == null) {
          PolarisGRPCGrpc.getHeartbeatMethod = getHeartbeatMethod =
              io.grpc.MethodDescriptor.<com.tencent.polaris.client.pb.ServiceProto.Instance, com.tencent.polaris.client.pb.ResponseProto.Response>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Heartbeat"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.tencent.polaris.client.pb.ServiceProto.Instance.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.tencent.polaris.client.pb.ResponseProto.Response.getDefaultInstance()))
              .setSchemaDescriptor(new PolarisGRPCMethodDescriptorSupplier("Heartbeat"))
              .build();
        }
      }
    }
    return getHeartbeatMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static PolarisGRPCStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PolarisGRPCStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PolarisGRPCStub>() {
        @java.lang.Override
        public PolarisGRPCStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PolarisGRPCStub(channel, callOptions);
        }
      };
    return PolarisGRPCStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static PolarisGRPCBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PolarisGRPCBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PolarisGRPCBlockingStub>() {
        @java.lang.Override
        public PolarisGRPCBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PolarisGRPCBlockingStub(channel, callOptions);
        }
      };
    return PolarisGRPCBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static PolarisGRPCFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PolarisGRPCFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PolarisGRPCFutureStub>() {
        @java.lang.Override
        public PolarisGRPCFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PolarisGRPCFutureStub(channel, callOptions);
        }
      };
    return PolarisGRPCFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class PolarisGRPCImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * 客户端上报
     * </pre>
     */
    public void reportClient(com.tencent.polaris.client.pb.ClientProto.Client request,
        io.grpc.stub.StreamObserver<com.tencent.polaris.client.pb.ResponseProto.Response> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getReportClientMethod(), responseObserver);
    }

    /**
     * <pre>
     * 被调方注册服务实例
     * </pre>
     */
    public void registerInstance(com.tencent.polaris.client.pb.ServiceProto.Instance request,
        io.grpc.stub.StreamObserver<com.tencent.polaris.client.pb.ResponseProto.Response> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRegisterInstanceMethod(), responseObserver);
    }

    /**
     * <pre>
     * 被调方反注册服务实例
     * </pre>
     */
    public void deregisterInstance(com.tencent.polaris.client.pb.ServiceProto.Instance request,
        io.grpc.stub.StreamObserver<com.tencent.polaris.client.pb.ResponseProto.Response> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeregisterInstanceMethod(), responseObserver);
    }

    /**
     * <pre>
     * 统一发现接口
     * </pre>
     */
    public io.grpc.stub.StreamObserver<com.tencent.polaris.client.pb.RequestProto.DiscoverRequest> discover(
        io.grpc.stub.StreamObserver<com.tencent.polaris.client.pb.ResponseProto.DiscoverResponse> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getDiscoverMethod(), responseObserver);
    }

    /**
     * <pre>
     * 被调方上报心跳
     * </pre>
     */
    public void heartbeat(com.tencent.polaris.client.pb.ServiceProto.Instance request,
        io.grpc.stub.StreamObserver<com.tencent.polaris.client.pb.ResponseProto.Response> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getHeartbeatMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getReportClientMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.tencent.polaris.client.pb.ClientProto.Client,
                com.tencent.polaris.client.pb.ResponseProto.Response>(
                  this, METHODID_REPORT_CLIENT)))
          .addMethod(
            getRegisterInstanceMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.tencent.polaris.client.pb.ServiceProto.Instance,
                com.tencent.polaris.client.pb.ResponseProto.Response>(
                  this, METHODID_REGISTER_INSTANCE)))
          .addMethod(
            getDeregisterInstanceMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.tencent.polaris.client.pb.ServiceProto.Instance,
                com.tencent.polaris.client.pb.ResponseProto.Response>(
                  this, METHODID_DEREGISTER_INSTANCE)))
          .addMethod(
            getDiscoverMethod(),
            io.grpc.stub.ServerCalls.asyncBidiStreamingCall(
              new MethodHandlers<
                com.tencent.polaris.client.pb.RequestProto.DiscoverRequest,
                com.tencent.polaris.client.pb.ResponseProto.DiscoverResponse>(
                  this, METHODID_DISCOVER)))
          .addMethod(
            getHeartbeatMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.tencent.polaris.client.pb.ServiceProto.Instance,
                com.tencent.polaris.client.pb.ResponseProto.Response>(
                  this, METHODID_HEARTBEAT)))
          .build();
    }
  }

  /**
   */
  public static final class PolarisGRPCStub extends io.grpc.stub.AbstractAsyncStub<PolarisGRPCStub> {
    private PolarisGRPCStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PolarisGRPCStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PolarisGRPCStub(channel, callOptions);
    }

    /**
     * <pre>
     * 客户端上报
     * </pre>
     */
    public void reportClient(com.tencent.polaris.client.pb.ClientProto.Client request,
        io.grpc.stub.StreamObserver<com.tencent.polaris.client.pb.ResponseProto.Response> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getReportClientMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * 被调方注册服务实例
     * </pre>
     */
    public void registerInstance(com.tencent.polaris.client.pb.ServiceProto.Instance request,
        io.grpc.stub.StreamObserver<com.tencent.polaris.client.pb.ResponseProto.Response> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRegisterInstanceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * 被调方反注册服务实例
     * </pre>
     */
    public void deregisterInstance(com.tencent.polaris.client.pb.ServiceProto.Instance request,
        io.grpc.stub.StreamObserver<com.tencent.polaris.client.pb.ResponseProto.Response> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeregisterInstanceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * 统一发现接口
     * </pre>
     */
    public io.grpc.stub.StreamObserver<com.tencent.polaris.client.pb.RequestProto.DiscoverRequest> discover(
        io.grpc.stub.StreamObserver<com.tencent.polaris.client.pb.ResponseProto.DiscoverResponse> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncBidiStreamingCall(
          getChannel().newCall(getDiscoverMethod(), getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     * 被调方上报心跳
     * </pre>
     */
    public void heartbeat(com.tencent.polaris.client.pb.ServiceProto.Instance request,
        io.grpc.stub.StreamObserver<com.tencent.polaris.client.pb.ResponseProto.Response> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getHeartbeatMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class PolarisGRPCBlockingStub extends io.grpc.stub.AbstractBlockingStub<PolarisGRPCBlockingStub> {
    private PolarisGRPCBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PolarisGRPCBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PolarisGRPCBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * 客户端上报
     * </pre>
     */
    public com.tencent.polaris.client.pb.ResponseProto.Response reportClient(com.tencent.polaris.client.pb.ClientProto.Client request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getReportClientMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * 被调方注册服务实例
     * </pre>
     */
    public com.tencent.polaris.client.pb.ResponseProto.Response registerInstance(com.tencent.polaris.client.pb.ServiceProto.Instance request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRegisterInstanceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * 被调方反注册服务实例
     * </pre>
     */
    public com.tencent.polaris.client.pb.ResponseProto.Response deregisterInstance(com.tencent.polaris.client.pb.ServiceProto.Instance request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeregisterInstanceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * 被调方上报心跳
     * </pre>
     */
    public com.tencent.polaris.client.pb.ResponseProto.Response heartbeat(com.tencent.polaris.client.pb.ServiceProto.Instance request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getHeartbeatMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class PolarisGRPCFutureStub extends io.grpc.stub.AbstractFutureStub<PolarisGRPCFutureStub> {
    private PolarisGRPCFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PolarisGRPCFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PolarisGRPCFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * 客户端上报
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.tencent.polaris.client.pb.ResponseProto.Response> reportClient(
        com.tencent.polaris.client.pb.ClientProto.Client request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getReportClientMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * 被调方注册服务实例
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.tencent.polaris.client.pb.ResponseProto.Response> registerInstance(
        com.tencent.polaris.client.pb.ServiceProto.Instance request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRegisterInstanceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * 被调方反注册服务实例
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.tencent.polaris.client.pb.ResponseProto.Response> deregisterInstance(
        com.tencent.polaris.client.pb.ServiceProto.Instance request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeregisterInstanceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * 被调方上报心跳
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.tencent.polaris.client.pb.ResponseProto.Response> heartbeat(
        com.tencent.polaris.client.pb.ServiceProto.Instance request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getHeartbeatMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_REPORT_CLIENT = 0;
  private static final int METHODID_REGISTER_INSTANCE = 1;
  private static final int METHODID_DEREGISTER_INSTANCE = 2;
  private static final int METHODID_HEARTBEAT = 3;
  private static final int METHODID_DISCOVER = 4;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final PolarisGRPCImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(PolarisGRPCImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_REPORT_CLIENT:
          serviceImpl.reportClient((com.tencent.polaris.client.pb.ClientProto.Client) request,
              (io.grpc.stub.StreamObserver<com.tencent.polaris.client.pb.ResponseProto.Response>) responseObserver);
          break;
        case METHODID_REGISTER_INSTANCE:
          serviceImpl.registerInstance((com.tencent.polaris.client.pb.ServiceProto.Instance) request,
              (io.grpc.stub.StreamObserver<com.tencent.polaris.client.pb.ResponseProto.Response>) responseObserver);
          break;
        case METHODID_DEREGISTER_INSTANCE:
          serviceImpl.deregisterInstance((com.tencent.polaris.client.pb.ServiceProto.Instance) request,
              (io.grpc.stub.StreamObserver<com.tencent.polaris.client.pb.ResponseProto.Response>) responseObserver);
          break;
        case METHODID_HEARTBEAT:
          serviceImpl.heartbeat((com.tencent.polaris.client.pb.ServiceProto.Instance) request,
              (io.grpc.stub.StreamObserver<com.tencent.polaris.client.pb.ResponseProto.Response>) responseObserver);
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
        case METHODID_DISCOVER:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.discover(
              (io.grpc.stub.StreamObserver<com.tencent.polaris.client.pb.ResponseProto.DiscoverResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class PolarisGRPCBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    PolarisGRPCBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.tencent.polaris.client.pb.PolarisGRPCService.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("PolarisGRPC");
    }
  }

  private static final class PolarisGRPCFileDescriptorSupplier
      extends PolarisGRPCBaseDescriptorSupplier {
    PolarisGRPCFileDescriptorSupplier() {}
  }

  private static final class PolarisGRPCMethodDescriptorSupplier
      extends PolarisGRPCBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    PolarisGRPCMethodDescriptorSupplier(String methodName) {
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
      synchronized (PolarisGRPCGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new PolarisGRPCFileDescriptorSupplier())
              .addMethod(getReportClientMethod())
              .addMethod(getRegisterInstanceMethod())
              .addMethod(getDeregisterInstanceMethod())
              .addMethod(getDiscoverMethod())
              .addMethod(getHeartbeatMethod())
              .build();
        }
      }
    }
    return result;
  }
}
