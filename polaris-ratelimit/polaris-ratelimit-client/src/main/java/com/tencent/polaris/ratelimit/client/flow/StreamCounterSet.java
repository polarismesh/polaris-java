package com.tencent.polaris.ratelimit.client.flow;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * 计数器对象
 */
public class StreamCounterSet {

	private static final Logger LOG = LoggerFactory.getLogger(StreamCounterSet.class);

	/**
	 * 引用的KEY
	 */
	private final AtomicInteger reference = new AtomicInteger();

	private final Object resourceLock = new Object();

	/**
	 * 初始化记录
	 */
	private final Map<ServiceIdentifier, InitializeRecord> initRecords = new ConcurrentHashMap<>();

	/**
	 * 节点唯一标识
	 */
	private final HostIdentifier identifier;

	/**
	 * 当前的资源
	 */
	private final AtomicReference<StreamResource> currentStreamResource = new AtomicReference<>();


	public StreamCounterSet(HostIdentifier identifier) {
		this.identifier = identifier;
	}

	public HostIdentifier getIdentifier() {
		return identifier;
	}

	/**
	 * 获取同步阻塞的客户端
	 *
	 * @return 同步阻塞的客户端
	 */
	public StreamResource checkAndCreateResource() {
		StreamResource streamResource = currentStreamResource.get();
		if (null != streamResource && !streamResource.isEndStream()) {
			return streamResource;
		}
		synchronized (resourceLock) {
			streamResource = currentStreamResource.get();
			if (null == streamResource || streamResource.isEndStream()) {
				LOG.info("[RateLimit] stream resource for {} not exists or destroyed, start to create", identifier);
				streamResource = new StreamResource(identifier, initRecords);
				currentStreamResource.set(streamResource);
			}
			return streamResource;
		}
	}

	public void addReference() {
		reference.incrementAndGet();
	}

	public boolean decreaseReference() {
		int value = reference.decrementAndGet();
		if (value == 0) {
			synchronized (resourceLock) {
				StreamResource streamResource = currentStreamResource.get();
				if (null != streamResource && !streamResource.isEndStream()) {
					streamResource.closeStream(true);
				}
			}
			return true;
		}
		return false;
	}

	public void deleteInitRecord(ServiceIdentifier serviceIdentifier) {
		StreamResource streamResource = currentStreamResource.get();
		if (null != streamResource) {
			streamResource.deleteInitRecord(serviceIdentifier);
		}
	}

	/**
	 * 获取流式的异步客户端
	 *
	 * @param serviceIdentifier 服务标识
	 * @param window            限流窗口
	 * @return 异步客户端
	 */
	public StreamResource preCheckAsync(ServiceIdentifier serviceIdentifier,
			RateLimitWindow window) {
		StreamResource streamResource = checkAndCreateResource();

		InitializeRecord record = initRecords.get(serviceIdentifier);
		if (record == null) {
			record = streamResource.addInitRecord(serviceIdentifier, window);
		}

		if (!isInitExpired(record, window)) {
			//未超时，先不初始化
			return null;
		}
		LOG.info("[RateLimit] start to init {}, remote server {}", serviceIdentifier,
				streamResource.getHostIdentifier());
		record.setInitStartTimeMilli(System.currentTimeMillis());

		return streamResource;
	}

	private boolean isInitExpired(InitializeRecord initializeRecord, RateLimitWindow window) {
		if (null == initializeRecord || initializeRecord.getInitStartTimeMilli() == 0) {
			return true;
		}
		return System.currentTimeMillis() - initializeRecord.getInitStartTimeMilli() >= window.getRateLimitConfig()
				.getRemoteSyncTimeoutMilli();
	}
}
