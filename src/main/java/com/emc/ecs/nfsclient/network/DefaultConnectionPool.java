package com.emc.ecs.nfsclient.network;

import com.emc.ecs.nfsclient.rpc.RpcException;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class DefaultConnectionPool implements ConnectionPool {

    private static final int CONNECT_TIMEOUT = 10000; // 10 seconds
    private final int _maxChannels;
    private final boolean _usePrivilegedPort;

    private final Bootstrap bootstrap;
    private final ConcurrentHashMap<Integer, Connection> channelToConnection = new ConcurrentHashMap<>();

    public DefaultConnectionPool(String remoteHost, int remotePort, boolean usePrivilegedPort, int maxChannels) {
        _maxChannels = maxChannels;
        _usePrivilegedPort = usePrivilegedPort;

        bootstrap = new Bootstrap()
                .group(NetMgr.getInstance().getEventLoopGroup())
                .remoteAddress(remoteHost, remotePort)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        nioSocketChannel.pipeline().addFirst(new RPCRecordDecoder());
                    }
                });
    }

    //ToDo: check exception handling ..
    @Override
    public Connection getConnection() {
        Integer randomIdx = ThreadLocalRandom.current().nextInt(_maxChannels);
        return channelToConnection.computeIfAbsent(randomIdx, i -> {
            Connection conn = new Connection(i, bootstrap, _usePrivilegedPort);
            try {
                conn.connect();
                conn.getChannel().pipeline()
                        .addLast(new PooledConnectionLifecycleHandler(this, conn));
                return conn;
            } catch (RpcException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void dropConnection(Connection connection) {
        Connection conn = channelToConnection.remove(connection.getId());
        conn.getChannel().close();
        conn.notifyAllPendingSenders("Connection dropped");
    }

    @Override
    public void close() {
        channelToConnection.forEach((i, conn) -> conn.close());
    }

}
