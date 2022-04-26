package com.tencent.polaris.ratelimit.client.flow;

import com.tencent.polaris.logging.LoggerFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
    public StreamResource checkAndCreateResource(ServiceIdentifier serviceIdentifier,
            RateLimitWindow rateLimitWindow) {
        StreamResource streamResource = currentStreamResource.get();
        if (null != streamResource && !streamResource.isEndStream()) {
            return streamResource;
        }
        synchronized (resourceLock) {
            streamResource = currentStreamResource.get();
            if (null == streamResource || streamResource.isEndStream()) {
                LOG.info("[RateLimit] stream resource for {} not exists or destroyed, start to create", identifier);
                streamResource = new StreamResource(identifier);
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


}
