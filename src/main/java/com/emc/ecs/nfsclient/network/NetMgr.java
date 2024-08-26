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

import com.emc.ecs.nfsclient.rpc.RpcException;
import com.emc.ecs.nfsclient.rpc.Xdr;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Singleton class to manage all Connection instances
 * 
 * @author seibed
 */
public class NetMgr {

    private int _channelsPerSocket;

    /**
     * The single instance.
     */
    private static final NetMgr _instance = new NetMgr();

    /**
     * @return The instance.
     */
    public static NetMgr getInstance() {
        return _instance;
    }

    /**
     * Construct the private instance.
     */
    private NetMgr() {
        super();
    }

    /**
     * connection tracking map
     */
    private final ConcurrentHashMap<InetSocketAddress, ConnectionPool> _connectionMap = new ConcurrentHashMap<>();

    /**
     * privileged connection tracking map
     */
    private final ConcurrentHashMap<InetSocketAddress, ConnectionPool> _privilegedConnectionMap = new ConcurrentHashMap<>();

    /**
     * Netty helper instance.
     */
    private final EventLoopGroup eventLoopGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors()*2, getThreadFactory());

    public EventLoopGroup getEventLoopGroup() {
        return eventLoopGroup;
    }


    /**
     * @return a thread pool instance using the proper factory to create daemon threads
     */
    private static ExecutorService newThreadPool() {
        return Executors.newCachedThreadPool(getThreadFactory());
    }

    /**
     * @return a thread factory that creates daemon threads
     */
    private static ThreadFactory getThreadFactory() {
        return new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                return thread;
            }

        };

    }

    /**
     * Basic RPC call functionality only. Send the request, creating a new
     * connection as necessary, and return the raw Xdr returned.
     * 
     * @param serverIP
     *            The endpoint of the server being called.
     * @param port
     *            The remote host port being called for this operation.
     * @param usePrivilegedPort
     *            <ul>
     *            <li>If <code>true</code>, use a privileged local port (below
     *            1024) for RPC communication.</li>
     *            <li>If <code>false</code>, use any non-privileged local port
     *            for RPC communication.</li>
     *            </ul>
     * @param xdrRequest
     *            The Xdr data for the request.
     * @param timeout
     *            The timeout in seconds.
     * @return The Xdr data for the response.
     * @throws RpcException
     */
    public Xdr sendAndWait(String serverIP, int port, boolean usePrivilegedPort, Xdr xdrRequest, int timeout) throws RpcException {
        ConnectionPool pool = getConnectionPool(serverIP, port, usePrivilegedPort);
        Connection connection = pool.getConnection();

        return connection.sendAndWait(timeout, xdrRequest);
    }

    public void sendAsync(String serverIP, int port, boolean usePrivilegedPort, Xdr xdrRequest, Callback<Xdr> callback) throws RpcException {
        ConnectionPool pool = getConnectionPool(serverIP, port, usePrivilegedPort);
        Connection connection = pool.getConnection();

        connection.sendAsync(xdrRequest, callback);
    }

    private ConnectionPool getConnectionPool(String serverIP, int port, boolean usePrivilegedPort) {
        InetSocketAddress key = InetSocketAddress.createUnresolved(serverIP, port);

        Map<InetSocketAddress, ConnectionPool> connectionMap = usePrivilegedPort ? _privilegedConnectionMap : _connectionMap;

        return connectionMap.computeIfAbsent(key,
                (InetSocketAddress addr) -> new DefaultConnectionPool(serverIP, port, usePrivilegedPort, _channelsPerSocket));
    }

    /**
     * Remove a dropped connection from the map.
     * 
     * @param key
     *            The key
     */
    public void dropConnection(InetSocketAddress key) {
        _connectionMap.remove(key);
        _privilegedConnectionMap.remove(key);
    }

    /**
     * Called when the application is being shut down.
     */
    public void shutdown() {

        for (ConnectionPool connection : _connectionMap.values()) {
            try {
                connection.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        for (ConnectionPool connection : _privilegedConnectionMap.values()) {
            try {
                connection.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        eventLoopGroup.shutdownGracefully();
    }

    /**
     * Getter method for Factory access.
     * 
     * @return The factory.
     */
//    public ChannelFactory getFactory() {
//        return _factory;
//    }

    public void setChannelsPerSocket(int channelsPerSocket) {
        this._channelsPerSocket = channelsPerSocket;
    }
}
