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

package com.tencent.polaris.plugins.outlier.detector.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

public class NioTCPServer {

    private static final int TIMEOUT = 3000; // timeout in milliseconds

    private final AtomicBoolean stopped = new AtomicBoolean(false);

    private final int port;

    private final NioHandler nioHandler;

    public NioTCPServer(int port, NioHandlerImpl nioHandler) {
        this.port = port;
        this.nioHandler = nioHandler;
    }

    public void close() {
        stopped.set(true);
    }

    public void listenAndServ() {
        Selector selector = null;
        try {
            // Create a selector
            selector = Selector.open();

            // Create listening socket channel for each port and register selector with channels

            ServerSocketChannel listnChannel = ServerSocketChannel.open();
            listnChannel.socket().bind(new InetSocketAddress(port));
            listnChannel.configureBlocking(false); // must be nonblocking to register

            // Register selector with channel. The returned key is ignored
            listnChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (!stopped.get()) {
                // Wait for some channel to be ready (or timeout)
                if (selector.select(TIMEOUT) == 0) {
                    continue;
                }

                // Get iterator on set of keys with I/O to process
                Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();

                while (keyIter.hasNext()) {
                    SelectionKey key = keyIter.next();
                    // Server socket channel has pending connection requests?
                    if (key.isAcceptable()) {
                        nioHandler.handleAccept(key);
                    } else
                        // Client socket channel has pending data?
                        if (key.isReadable()) {
                            nioHandler.handleRead(key);
                        } else
                            // Client socket channel is available for writing and
                            // key is valid (i.e., channel not closed)?
                            if (key.isValid() && key.isWritable()) {
                                nioHandler.handleWrite(key);
                            }
                    keyIter.remove(); // remove from set of selected keys
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != selector) {
                try {
                    selector.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
    }
}
