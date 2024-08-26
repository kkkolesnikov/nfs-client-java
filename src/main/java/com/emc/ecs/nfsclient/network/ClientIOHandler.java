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

import com.emc.ecs.nfsclient.rpc.Xdr;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.NotYetConnectedException;

/**
 * Each TCP connection has a corresponding ClientIOHandler instance.
 * ClientIOHandler includes the handler to receiving data and connection
 * establishment
 * 
 * @author seibed
 */
public class ClientIOHandler extends SimpleChannelInboundHandler<ByteBuf> {

    /**
     * The usual logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ClientIOHandler.class);

    /**
     * The Netty helper class object.
     */

    /**
     * The connection instance
     */
    private final Connection _connection;

    /**
     * The only constructor.
     **            A Netty helper instance.
     */
    public ClientIOHandler(Connection owner) {
        _connection = owner;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf buf) {
        // remove marking
        Xdr x = RecordMarkingUtil.removeRecordMarking(buf.array());
        // remove the request from timeout manager map
        int xid = x.getXid();
        _connection.notifySender(xid, x);
    }

}
