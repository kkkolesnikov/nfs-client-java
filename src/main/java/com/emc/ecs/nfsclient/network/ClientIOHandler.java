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
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf buf) throws Exception {
        // remove marking
        Xdr x = RecordMarkingUtil.removeRecordMarking(buf.array());
        // remove the request from timeout manager map
        int xid = x.getXid();
        _connection.notifySender(xid, x);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        closeConnection("Channel inactive");
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        closeConnection("Channel unregistered");
    }

//    /**
//     * Convenience method to standardize connection closing.We never try to
//     * reconnect the tcp connections. the new connection will be launched when
//     * new request is received. Reasons:
//     * <ol>
//     * <li>Portmap service will disconnect a tcp connection once it has been
//     * idle for a few seconds.</li>
//     * <li>Mounting service is listening to a temporary port, the port will
//     * change after nfs server restart.</li>
//     * <li>Even Nfs server may be listening to a temporary port.</li>
//     * </ol>
//     *
//     * @param messageStart
//     *            A string used to start the log message.
//     */
    private void closeConnection(String messageStart) {
        LOG.warn("{}: {}", messageStart, _connection.getRemoteAddress());
        _connection.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable throwable) {
        // do not print exception if it is BindException.
        // we are trying to search available port below 1024. It is not good to
        // print a flood
        // of error logs during the searching.
        if (throwable instanceof java.net.BindException) {
            return;
        }
        //        if (!((cause instanceof NotYetConnectedException)
        //                && _connection.getConnectionState().equals(Connection.State.CONNECTING))) {
        //            ctx.getChannel().close();
        //       }

        if (!((throwable instanceof NotYetConnectedException))) {
            channelHandlerContext.channel().close();
        }
    }
}
