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

package com.tencent.polaris.api.utils;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 可关闭的读写锁
 */
public class ClosableReadWriteLock {

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public LockWrapper readLock() {
        return new LockWrapper(readWriteLock.readLock());
    }

    public LockWrapper writeLock() {
        return new LockWrapper(readWriteLock.writeLock());
    }

    /**
     * 锁封装，保证自动关闭
     */
    public static class LockWrapper implements AutoCloseable {

        private final Lock internalLock;

        public LockWrapper(Lock l) {
            this.internalLock = l;
        }

        public void lock() {
            this.internalLock.lock();
        }

        public void close() {
            this.internalLock.unlock();
        }
    }

}
