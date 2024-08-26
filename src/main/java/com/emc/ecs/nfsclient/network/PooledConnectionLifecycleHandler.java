package com.emc.ecs.nfsclient.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.channels.NotYetConnectedException;

public class PooledConnectionLifecycleHandler extends ChannelInboundHandlerAdapter {

    private final ConnectionPool pool;
    private final Connection connection;

    public PooledConnectionLifecycleHandler(ConnectionPool pool, Connection conn) {
        this.pool = pool;
        this.connection = conn;
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Channel unregistered");
        pool.dropConnection(connection);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Channel inactive");
        pool.dropConnection(connection);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable throwable) {
        //ToDo: figure out exceptions ..

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
