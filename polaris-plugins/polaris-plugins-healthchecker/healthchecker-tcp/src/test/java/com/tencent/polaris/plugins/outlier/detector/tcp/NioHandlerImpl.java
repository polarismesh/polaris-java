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
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class NioHandlerImpl implements NioHandler {

    private final int bufSize; // Size of I/O buffer

    public NioHandlerImpl() {
        this.bufSize = 256;
    }

    public void handleAccept(SelectionKey key) throws IOException {
        SocketChannel clientChannel = ((ServerSocketChannel) key.channel())
                .accept();
        // Must be nonblocking to register
        clientChannel.configureBlocking(false);
        // Register the selector with new channel for read and attach byte
        // buffer
        clientChannel.register(key.selector(), SelectionKey.OP_READ,
                ByteBuffer.allocate(bufSize));

    }

    public void handleRead(SelectionKey key) throws IOException {
        // Client socket channel has pending data
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buf = (ByteBuffer) key.attachment();
        long bytesRead = clientChannel.read(buf);

        if (bytesRead == -1) {
            clientChannel.close();
        } else if (bytesRead > 0) {
            System.out.println("bytes:" + bytesRead + ":Message: " + new String(buf.array()));
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        }
    }

    public void handleWrite(SelectionKey key) throws IOException {
        // Retrieve data read earlier
        ByteBuffer buf = (ByteBuffer) key.attachment();
        System.out.println("bytes to write, Message: " + new String(buf.array()));
        buf.flip(); // Prepare buffer for writing
        SocketChannel clientChannel = (SocketChannel) key.channel();
        clientChannel.write(buf);
        if (!buf.hasRemaining()) {
            // Nothing left, so no longer interested in writes
            clientChannel.close();
        }
        buf.compact(); // Make room for more data to be read in
    }
}
