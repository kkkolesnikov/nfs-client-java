package com.emc.ecs.nfsclient.network;

import com.emc.ecs.nfsclient.rpc.RpcException;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class ConnectionPool {

    private final ArrayList<Connection> pool;

    public ConnectionPool(String remoteHost, int port, boolean usePrivilegedPort, int n) throws RpcException {
        this.pool = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            final Connection conn = new Connection(remoteHost, port, usePrivilegedPort);
            conn.connect();
            this.pool.add(i, conn);
        }
    }

    public Connection getConnection() {
        int randConnIdx = ThreadLocalRandom.current().nextInt(this.pool.size());
        return pool.get(randConnIdx);
    }

    public void shutdown() {
        pool.forEach(connection -> connection.shutdown());
    }
}
