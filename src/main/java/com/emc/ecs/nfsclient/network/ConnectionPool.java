package com.emc.ecs.nfsclient.network;

import com.emc.ecs.nfsclient.rpc.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public class ConnectionPool {

    private final ConcurrentHashMap<Integer, Connection> pool;
    private final String remoteHost;
    private final int remotePort;
    private final boolean isPrivileged;
    private final int maxConnections;

    public ConnectionPool(String remoteHost, int port, boolean usePrivilegedPort, int n) {
        this.remoteHost = remoteHost;
        this.remotePort = port;
        this.isPrivileged = usePrivilegedPort;
        this.maxConnections = n;
        this.pool = new ConcurrentHashMap<>();
    }

    public Connection getConnection() {
        int randConnIdx = ThreadLocalRandom.current().nextInt(maxConnections);

        return pool.computeIfAbsent(randConnIdx, k -> {
            Connection conn = new Connection(this, k, this.remoteHost, this.remotePort, this.isPrivileged);
            try {
                conn.connect();
            } catch (RpcException e) {
                return null;
            }
            return conn;
        });
    }

    public <T>T withConnection(Function<Connection, T> f) {
        return f.apply(getConnection());
    }

    public void dropConnection(Connection connection) {
        pool.remove(connection.getId());
    }

    public void shutdown() {
        pool.forEach((i, c) -> c.shutdown());
    }
}
