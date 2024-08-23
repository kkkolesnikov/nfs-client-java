/**
 * Copyright 2016-2018 Dell Inc. or its subsidiaries. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.emc.ecs.nfsclient.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

/**
 * To receive the entire response. We do not actually decode the rpc packet here.
 * Just get the size from the packet and then put them in internal buffer until all data arrive.
 * 
 * @author seibed
 */
public class RPCRecordDecoder extends ByteToMessageDecoder {

    /**
     * Holds the calculated record length for each channel until the Channel is ready for buffering.
     * Reset to 0 after that for the next channel.
     */
    private int _recordLength = 0;

    public RPCRecordDecoder() {}

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
//        // Wait until the length prefix is available.
        if (byteBuf.readableBytes() < 4) {
            return;
        }

        byteBuf.markReaderIndex(); //.markReaderIndex();

        long fragSize = byteBuf.readUnsignedInt();
        boolean lastFragment = RecordMarkingUtil.isLastFragment(fragSize);
        fragSize = RecordMarkingUtil.maskFragmentSize(fragSize);
        if (byteBuf.readableBytes() < fragSize) {
            byteBuf.resetReaderIndex();
            return;
        }

        byteBuf.skipBytes((int) fragSize);
//
        _recordLength += 4 + (int) fragSize;
//
//        //check the last fragment
        if (!lastFragment) {
            //not the last fragment, the data is put in an internally maintained cumulative buffer
             return;
        }
//
        ByteBuf rpcResponse = Unpooled.buffer(_recordLength, _recordLength);
        byteBuf.readerIndex(byteBuf.readerIndex() - _recordLength);
        byteBuf.readBytes(rpcResponse, 0, _recordLength);
//
        _recordLength = 0;
        list.add(rpcResponse);
    }
}
